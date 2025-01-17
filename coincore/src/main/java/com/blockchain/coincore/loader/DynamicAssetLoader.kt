package com.blockchain.coincore.loader

import com.blockchain.coincore.Asset
import com.blockchain.coincore.CoincoreInitFailure
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.NonCustodialSupport
import com.blockchain.coincore.custodialonly.DynamicOnlyTradingAsset
import com.blockchain.coincore.erc20.Erc20Asset
import com.blockchain.coincore.evm.L1EvmAsset
import com.blockchain.coincore.fiat.FiatAsset
import com.blockchain.coincore.impl.EthHotWalletAddressResolver
import com.blockchain.coincore.impl.StandardL1Asset
import com.blockchain.coincore.selfcustody.DynamicSelfCustodyAsset
import com.blockchain.coincore.selfcustody.StxAsset
import com.blockchain.coincore.wrap.FormatUtilities
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.erc20.data.store.L1BalanceStore
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrThrow
import com.blockchain.outcome.map
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.isCustodial
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isNonCustodial
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.await
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.filterList
import piuk.blockchain.androidcore.utils.extensions.filterListItemIsInstance
import piuk.blockchain.androidcore.utils.extensions.mapList
import piuk.blockchain.androidcore.utils.extensions.zipSingles
import timber.log.Timber

// This is a rubbish regex, but it'll do until I'm provided a better one
private const val defaultCustodialAddressValidation = "[a-zA-Z0-9]{15,}"

internal class DynamicAssetLoader(
    private val nonCustodialAssets: Set<CryptoAsset>,
    private val experimentalL1EvmAssets: Set<CryptoCurrency>,
    private val assetCatalogue: AssetCatalogueImpl,
    private val payloadManager: PayloadDataManager,
    private val l1BalanceStore: L1BalanceStore,
    private val erc20DataManager: Erc20DataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatusPrefs,
    private val tradingService: TradingService,
    private val interestService: InterestService,
    private val labels: DefaultLabels,
    private val custodialWalletManager: CustodialWalletManager,
    private val remoteLogger: RemoteLogger,
    private val formatUtils: FormatUtilities,
    private val identityAddressResolver: IdentityAddressResolver,
    private val ethHotWalletAddressResolver: EthHotWalletAddressResolver,
    private val selfCustodyService: NonCustodialService,
    private val layerTwoFeatureFlag: FeatureFlag,
    private val stxForAllFeatureFlag: FeatureFlag,
) : AssetLoader {

    private val assetMap = mutableMapOf<Currency, Asset>()
    override operator fun get(asset: Currency): Asset =
        assetMap[asset] ?: attemptLoadAsset(asset)

    private fun attemptLoadAsset(currency: Currency): Asset =
        when {
            currency.isErc20() -> loadErc20Asset(currency)
            (currency as? AssetInfo)?.isCustodialOnly == true -> loadCustodialOnlyAsset(currency)
            (currency as? AssetInfo)?.isNonCustodial == true -> loadSelfCustodialAsset(currency)
            currency is FiatCurrency -> FiatAsset(currency)
            else -> throw IllegalStateException("Unknown asset type enabled: ${currency.networkTicker}")
        }.also {
            check(currency !in assetMap.keys) { "Asset already loaded" }
            assetMap[currency] = it
        }

    private val enabledEvmL1Assets: Single<Set<CryptoAsset>>
        get() {
            return Singles.zip(
                layerTwoFeatureFlag.enabled,
                erc20DataManager.getSupportedNetworks()
            ).map { (isEnabled, evmNetworks) ->
                if (isEnabled) {
                    experimentalL1EvmAssets.filter { evmAsset ->
                        evmNetworks.find {
                            it.networkTicker == evmAsset.networkTicker ||
                                it.networkTicker == evmAsset.displayTicker
                        } != null
                    }.map { asset ->
                        L1EvmAsset(
                            currency = asset,
                            l1BalanceStore = l1BalanceStore,
                            erc20DataManager = erc20DataManager,
                            feeDataManager = feeDataManager,
                            walletPreferences = walletPreferences,
                            labels = labels,
                            formatUtils = formatUtils,
                            addressResolver = ethHotWalletAddressResolver,
                            layerTwoFeatureFlag = layerTwoFeatureFlag
                        )
                    }.toSet()
                } else {
                    emptySet()
                }
            }
        }

    /*
    * This methods responsibility is discover and persist the available assets that the application uses
    * More specifically after it discovers it persists two sets of currencies (Assets). The first list
    * consists of all the available assets that the app supports and the second persists the active assets of our
    * application. As active we consider any of the following assets:
    * - All the big standard PKWs L1s that are currently hardcoded in the App [BTC,BCH,ETH,XLM,MATIC]
    * - All the PKW erc20s that user has a balance [erc20Datamanager.getActive()]
    * - All custodial currencies that user has a trading OR an interest balance
    * - All PKW subscriptions [selfCustodyService.getSubscriptions()]
    * Every asset loads the corresponding accounts, based on what it supports.
    * */
    override fun initAndPreload(): Completable {
        return Singles.zip(
            assetCatalogue.initialise(),
            enabledEvmL1Assets
        )
            .doOnSubscribe { remoteLogger.logEvent("Coincore init started") }
            .flatMap { (supportedAssets, supportedEvmL1Assets) ->
                initNonCustodialAssets(nonCustodialAssets.plus(supportedEvmL1Assets)).toSingle {
                    loadErc20AndCustodialAssets(
                        allAssets = supportedAssets
                    )
                }.map { loadedAssets ->
                    nonCustodialAssets + supportedEvmL1Assets + loadedAssets
                }
            }
            .doOnSuccess { assetList ->
                assetList.map { it.currency.networkTicker }.let { ids ->
                    /**
                     * checking that values here are unique
                     */
                    check(ids.size == ids.toSet().size)
                }
                /**
                 * Persisting to loaded any custodial+the standardL1s
                 */
                assetMap.putAll(
                    assetList.filter { (it.currency as? AssetInfo)?.isCustodial == true || it is StandardL1Asset }
                        .associateBy { it.currency }
                )
            }
            .doOnError { Timber.e("init failed") }
            .ignoreElement()
    }

    /*
    * Init the local non custodial assets currently BCH and ETH that require
    * metadata initialisation.
    * We need to make sure ETH is initialised before we request the supported erc20s
    * */
    private fun initNonCustodialAssets(assetList: Set<CryptoAsset>): Completable =
        assetList.filterIsInstance<NonCustodialSupport>().map {
            Single.defer { it.initToken().toSingle { } }.doOnError {
                remoteLogger.logException(
                    CoincoreInitFailure(
                        "Failed init: ${(it as CryptoAsset).currency.networkTicker}", it
                    )
                )
            }
        }.zipSingles().subscribeOn(Schedulers.io()).ignoreElement()

    @OptIn(FlowPreview::class)
    private fun loadSelfCustodialAssets(): Flow<List<CryptoAsset>> {
        return selfCustodyService.getSubscriptions()
            .flatMapConcat { result ->
                when (result) {
                    is Outcome.Success ->
                        flow {
                            emit(
                                result.value.mapNotNull {
                                    assetCatalogue.assetInfoFromNetworkTicker(it)?.let { asset ->
                                        loadSelfCustodialAsset(asset)
                                    }
                                }
                            )
                        }
                    is Outcome.Failure -> {
                        // getSubscriptions might fail because the wallet has never called auth before
                        val isSuccess = selfCustodyService.authenticate().getOrThrow()
                        if (isSuccess) {
                            selfCustodyService.getSubscriptions().map { subscriptions ->
                                subscriptions.getOrThrow().mapNotNull {
                                    assetCatalogue.assetInfoFromNetworkTicker(it)?.let { asset ->
                                        loadSelfCustodialAsset(asset)
                                    }
                                }
                            }
                        } else {
                            throw IllegalStateException("Could not load subscriptions")
                        }
                    }
                }
            }
    }

    private fun loadErc20AndCustodialAssets(allAssets: Set<Currency>): List<Asset> =
        allAssets.mapNotNull { currency ->
            when {
                currency.isErc20() -> loadErc20Asset(currency)
                currency is AssetInfo && currency.isCustodialOnly -> loadCustodialOnlyAsset(currency)
                currency is FiatCurrency -> FiatAsset(currency)
                else -> null
            }
        }

    /**
     * We need to request:
     * - All erc20 with balance.
     * - All trading with balance.
     * - All interest with balance.
     * */

    private fun loadNonCustodialActiveAssets(): Flow<List<Asset>> {
        val activePKWErc20sFlow = erc20DataManager.getActiveAssets()
            .filterList { it.isErc20() }
            .mapList { loadErc20Asset(it) }
            .catch { emit(emptyList()) }

        val selfCustodialAssetsFlow = loadSelfCustodialAssets()
            .filterList {
                it.currency.networkTicker !in nonCustodialAssets.toList().map { asset -> asset.currency.networkTicker }
            }
            .catch { emit(emptyList()) }

        val enabledL1AssetsFlow = flow {
            val evmAssets = enabledEvmL1Assets.await()
            emit(nonCustodialAssets.plus(evmAssets.toSet()))
        }

        return combine(
            activePKWErc20sFlow,
            selfCustodialAssetsFlow,
            enabledL1AssetsFlow
        ) { activePKWErc20s, dynamicSelfCustodyAssets, standardAssets ->
            activePKWErc20s + dynamicSelfCustodyAssets + standardAssets
        }
    }

    private fun loadCustodialActiveAssets(): Flow<List<Asset>> {
        val activeTradingFlow = tradingService.getActiveAssets()
            .filterListItemIsInstance<AssetInfo>()
            .mapList { loadCustodialOnlyAsset(it) }
            .catch { emit(emptyList()) }

        val activeInterestFlow = interestService.getActiveAssets()
            .mapList { loadCustodialOnlyAsset(it) }
            .catch { emit(emptyList()) }

        val supportedFiatsFlow = custodialWalletManager.getSupportedFundsFiats()
            .mapList { FiatAsset(currency = it) }
            .catch { emit(emptyList()) }

        return combine(
            activeTradingFlow,
            activeInterestFlow,
            supportedFiatsFlow
        ) { activeTrading, activeInterest, supportedFiats ->
            activeTrading +
                activeInterest.filter { it.currency !in activeTrading.map { active -> active.currency } } +
                supportedFiats
        }
    }

    private fun loadCustodialOnlyAsset(assetInfo: AssetInfo): CryptoAsset {
        return DynamicOnlyTradingAsset(
            currency = assetInfo,
            addressValidation = defaultCustodialAddressValidation
        )
    }

    private fun loadSelfCustodialAsset(assetInfo: AssetInfo): CryptoAsset {
        // TODO(dtverdota): Remove Stx-specific code once it is enabled for all users
        return if (assetInfo.networkTicker == "STX" && stxForAllFeatureFlag.isEnabled) {
            StxAsset(
                currency = assetInfo,
                payloadManager = payloadManager,
                addressValidation = defaultCustodialAddressValidation,
                addressResolver = identityAddressResolver,
                selfCustodyService = selfCustodyService,
                stxForAllFeatureFlag = stxForAllFeatureFlag,
                walletPreferences = walletPreferences
            )
        } else {
            return DynamicSelfCustodyAsset(
                currency = assetInfo,
                payloadManager = payloadManager,
                addressValidation = defaultCustodialAddressValidation,
                addressResolver = identityAddressResolver,
                selfCustodyService = selfCustodyService,
                walletPreferences = walletPreferences
            )
        }
    }

    private fun loadErc20Asset(assetInfo: Currency): CryptoAsset {
        require(assetInfo is AssetInfo)
        require(assetInfo.isErc20())
        return Erc20Asset(
            currency = assetInfo,
            erc20DataManager = erc20DataManager,
            feeDataManager = feeDataManager,
            labels = labels,
            walletPreferences = walletPreferences,
            formatUtils = formatUtils,
            addressResolver = ethHotWalletAddressResolver,
            layerTwoFeatureFlag = layerTwoFeatureFlag
        )
    }

    override fun activeAssets(walletMode: WalletMode): Flow<List<Asset>> {
        return when (walletMode) {
            WalletMode.CUSTODIAL_ONLY -> loadCustodialActiveAssets()
            WalletMode.NON_CUSTODIAL_ONLY -> loadNonCustodialActiveAssets()
            WalletMode.UNIVERSAL -> allActive()
        }
    }

    private fun allActive(): Flow<List<Asset>> {
        val nonCustodialFlow = loadNonCustodialActiveAssets()
        val custodialFlow = loadCustodialActiveAssets()

        return combine(nonCustodialFlow, custodialFlow) { nonCustodial, custodial ->
            // remove any asset from custodial list that already exists in non custodial
            val uniqueCustodial = custodial.filter {
                it.currency.networkTicker !in nonCustodial.map { asset -> asset.currency.networkTicker }
            }

            // merge all
            (nonCustodial.map { it.currency } + uniqueCustodial.map { it.currency })
                .map { this[it] }
        }
    }

    override val loadedAssets: List<Asset>
        get() = assetMap.values.toList()
}
