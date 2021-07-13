package piuk.blockchain.android.coincore.erc20

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.erc20.Erc20Transfer
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

@Suppress("ClassName")
private object DUMMY_ERC20_TOKEN : CryptoCurrency(
    ticker = "DUMMY",
    name = "Dummies",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 8,
    requiredConfirmations = 5,
    colour = "#123456"
)

class Erc20AccountActivityTest {

    private val payloadManager: PayloadDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val walletPreferences: WalletStatus = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()

    private val subject = Erc20NonCustodialAccount(
        asset = DUMMY_ERC20_TOKEN,
        payloadManager = payloadManager,
        label = "Text Dgld Account",
        address = "Test Dgld Address",
        fees = mock(),
        ethDataManager = ethDataManager,
        exchangeRates = exchangeRates,
        walletPreferences = walletPreferences,
        custodialWalletManager = custodialWalletManager,
        identity = mock()
    )

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
    }

    @Test
    fun getErc20TransactionsList() {
        val erc20Transfer = Erc20Transfer(
            logIndex = "132",
            from = "0x4058a004dd718babab47e14dd0d744742e5b9903",
            to = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29",
            value = 10000.toBigInteger(),
            transactionHash = "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff",
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297
        )

        val swapSummary = TradeTransactionItem(
            "123",
            1L,
            TransferDirection.ON_CHAIN,
            "sendingAddress",
            "receivingAddress",
            CustodialOrderState.FINISHED,
            CryptoValue.zero(DUMMY_ERC20_TOKEN),
            CryptoValue.zero(CryptoCurrency.BTC),
            CryptoValue.zero(CryptoCurrency.BTC),
            CurrencyPair.CryptoCurrencyPair(DUMMY_ERC20_TOKEN, CryptoCurrency.BTC),
            FiatValue.zero("USD"),
            "USD"
        )

        val summaryList = listOf(swapSummary)

        whenever(ethDataManager.getErc20Transactions(DUMMY_ERC20_TOKEN))
            .thenReturn(Observable.just(listOf(erc20Transfer)))

        whenever(
            ethDataManager
                .getTransaction("0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff")
        )
            .thenReturn(
                Observable.just(
                    EthTransaction(
                        gasPrice = 100.toBigInteger(),
                        gasUsed = 2.toBigInteger()
                    )
                )
            )

        whenever(ethDataManager.fetchErc20DataModel(DUMMY_ERC20_TOKEN))
            .thenReturn(Observable.just(mock()))

        whenever(ethDataManager.getErc20AccountHash(DUMMY_ERC20_TOKEN))
            .thenReturn(Single.just("0x4058a004dd718babab47e14dd0d744742e5b9903"))

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Single.just(
                EthLatestBlockNumber().apply {
                    number = erc20Transfer.blockNumber.plus(3.toBigInteger())
                }
            )
            )

        whenever(custodialWalletManager.getCustodialActivityForAsset(any(), any()))
            .thenReturn(Single.just(summaryList))

        subject.activity
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.size == 1 && it[0].run {
                    this is Erc20ActivitySummaryItem &&
                        asset == DUMMY_ERC20_TOKEN &&
                        !doubleSpend &&
                        !isFeeTransaction &&
                        confirmations == 3 &&
                        timeStampMs == 1557334297000L &&
                        transactionType == TransactionSummary.TransactionType.SENT &&
                        txId == "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff" &&
                        confirmations == 3 &&
                        value == CryptoValue.fromMinor(DUMMY_ERC20_TOKEN, 10000.toBigInteger()) &&
                        inputsMap["0x4058a004dd718babab47e14dd0d744742e5b9903"] ==
                        CryptoValue.fromMinor(DUMMY_ERC20_TOKEN, 10000.toBigInteger()) &&
                        outputsMap["0x2ca28ffadd20474ffe2705580279a1e67cd10a29"] ==
                        CryptoValue.fromMinor(DUMMY_ERC20_TOKEN, 10000.toBigInteger())
                }
            }

        verify(ethDataManager).getErc20Transactions(DUMMY_ERC20_TOKEN)
    }
}
