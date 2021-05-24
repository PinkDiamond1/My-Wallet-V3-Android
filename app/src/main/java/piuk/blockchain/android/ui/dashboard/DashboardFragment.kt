package piuk.blockchain.android.ui.dashboard

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.preferences.CurrencyPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.databinding.FragmentDashboardBinding
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.simplebuy.BuySellType
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyCancelOrderBottomSheet
import piuk.blockchain.android.ui.airdrops.AirdropStatusSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow
import piuk.blockchain.android.ui.dashboard.assetdetails.assetActionEvent
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.dashboard.sheets.FiatFundsDetailSheet
import piuk.blockchain.android.ui.dashboard.sheets.ForceBackupForSendSheet
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.interest.InterestSummarySheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.BankLinkingHost
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.util.launchUrlInBrowser
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class EmptyDashboardItem : DashboardItem

private typealias RefreshFn = () -> Unit

class DashboardFragment :
    HomeScreenMviFragment<DashboardModel, DashboardIntent, DashboardState, FragmentDashboardBinding>(),
    ForceBackupForSendSheet.Host,
    FiatFundsDetailSheet.Host,
    KycBenefitsBottomSheet.Host,
    DialogFlow.FlowHost,
    AssetDetailsFlow.AssetDetailsHost,
    InterestSummarySheet.Host,
    BankLinkingHost {

    override val model: DashboardModel by scopedInject()
    private val announcements: AnnouncementList by scopedInject()
    private val analyticsReporter: BalanceAnalyticsReporter by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val coincore: Coincore by scopedInject()
    private val assetResources: AssetResources by scopedInject()

    private val theAdapter: DashboardDelegateAdapter by lazy {
        DashboardDelegateAdapter(
            prefs = get(),
            onCardClicked = { onAssetClicked(it) },
            analytics = get(),
            onFundsItemClicked = { onFundsClicked(it) },
            coincore = coincore,
            assetResources = assetResources
        )
    }

    private lateinit var theLayoutManager: RecyclerView.LayoutManager

    private val displayList = mutableListOf<DashboardItem>()

    private val compositeDisposable = CompositeDisposable()
    private val rxBus: RxBus by inject()

    private val actionEvent by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private val flowToLaunch: AssetAction? by unsafeLazy {
        arguments?.getSerializable(FLOW_TO_LAUNCH) as? AssetAction
    }

    private val flowCurrency: String? by unsafeLazy {
        arguments?.getString(FLOW_FIAT_CURRENCY)
    }

    private var state: DashboardState? =
        null // Hold the 'current' display state, to enable optimising of state updates

    @UiThread
    override fun render(newState: DashboardState) {
        try {
            doRender(newState)
        } catch (e: Throwable) {
            Timber.e("Error rendering: $e")
        }
    }

    @UiThread
    private fun doRender(newState: DashboardState) {

        binding.swipe.isRefreshing = false

        if (newState.assets.isNotEmpty()) {
            if (displayList.isEmpty()) {
                createDisplayList(newState)
            } else {
                updateDisplayList(newState)
            }
        } else {
            // TODO clear display list
        }

        if (this.state?.dashboardNavigationAction != newState.dashboardNavigationAction) {
            handleStateNavigation(newState)
        }

        // Update/show dialog flow
        if (state?.activeFlow != newState.activeFlow) {
            state?.activeFlow?.let {
                clearBottomSheet()
            }

            newState.activeFlow?.startFlow(childFragmentManager, this)
        }

        // Update/show announcement
        if (this.state?.announcement != newState.announcement) {
            showAnnouncement(newState.announcement)
        }

        updateAnalytics(this.state, newState)

        binding.dashboardProgress.visibleIf { newState.hasLongCallInProgress }

        this.state = newState
    }

    private fun createDisplayList(newState: DashboardState) {
        with(displayList) {
            add(IDX_CARD_ANNOUNCE, EmptyDashboardItem()) // Placeholder for announcements
            add(IDX_CARD_BALANCE, newState)
            add(IDX_FUNDS_BALANCE, EmptyDashboardItem()) // Placeholder for funds
            addAll(newState.assets.values)
        }
        theAdapter.notifyDataSetChanged()
    }

    private fun updateDisplayList(newState: DashboardState) {
        with(displayList) {

            val modList = mutableListOf<RefreshFn?>()
            newState.assets.values.forEachIndexed { index, v ->
                modList.add(handleUpdatedAssetState(IDX_ASSET_CARDS_START + index, v))
            }

            modList.removeAll { it == null }

            if (newState.fiatAssets?.fiatAccounts?.isNotEmpty() == true) {
                set(IDX_FUNDS_BALANCE, newState.fiatAssets)
                modList.add { theAdapter.notifyItemChanged(IDX_FUNDS_BALANCE) }
            }

            if (modList.isNotEmpty()) {
                set(IDX_CARD_BALANCE, newState)
                modList.add { theAdapter.notifyItemChanged(IDX_CARD_BALANCE) }
            }

            modList.forEach { it?.invoke() }
        }
    }

    private fun handleUpdatedAssetState(index: Int, newState: CryptoAssetState): RefreshFn? {
        if (displayList[index] != newState) {
            displayList[index] = newState
            return { theAdapter.notifyItemChanged(index) }
        } else {
            return null
        }
    }

    private fun handleStateNavigation(state: DashboardState) {
        when {
            state.dashboardNavigationAction?.isBottomSheet() == true -> {
                handleBottomSheet(state)
                model.process(ResetDashboardNavigation)
            }
            state.dashboardNavigationAction is LinkBankNavigationAction -> {
                startBankLinking(state.dashboardNavigationAction)
            }
        }
    }

    private fun startBankLinking(action: DashboardNavigationAction) {
        (action as? DashboardNavigationAction.LinkBankWithPartner)?.let {
            startActivityForResult(
                BankAuthActivity.newInstance(
                    action.linkBankTransfer,
                    when (it.assetAction) {
                        AssetAction.FiatDeposit -> {
                            BankAuthSource.DEPOSIT
                        }
                        AssetAction.Withdraw -> {
                            BankAuthSource.WITHDRAW
                        }
                        else -> {
                            throw IllegalStateException("Attempting to link from an unsupported action")
                        }
                    },
                    requireContext()
                ),
                BankAuthActivity.LINK_BANK_REQUEST_CODE
            )
        }
    }

    private fun handleBottomSheet(state: DashboardState) {
        showBottomSheet(
            when (state.dashboardNavigationAction) {
                DashboardNavigationAction.StxAirdropComplete -> AirdropStatusSheet.newInstance(
                    blockstackCampaignName
                )
                DashboardNavigationAction.BackUpBeforeSend -> ForceBackupForSendSheet.newInstance(
                    state.backupSheetDetails!!
                )
                DashboardNavigationAction.SimpleBuyCancelOrder -> {
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_PROMPT)
                    SimpleBuyCancelOrderBottomSheet.newInstance(true)
                }
                DashboardNavigationAction.FiatFundsDetails -> FiatFundsDetailSheet.newInstance(
                    state.selectedFiatAccount
                        ?: return
                )
                DashboardNavigationAction.LinkOrDeposit -> {
                    state.selectedFiatAccount?.let {
                        WireTransferAccountDetailsBottomSheet.newInstance(it)
                    } ?: WireTransferAccountDetailsBottomSheet.newInstance()
                }
                DashboardNavigationAction.PaymentMethods -> {
                    state.linkablePaymentMethodsForAction?.let {
                        LinkBankMethodChooserBottomSheet.newInstance(it)
                    }
                }
                DashboardNavigationAction.FiatFundsNoKyc -> showFiatFundsKyc()
                DashboardNavigationAction.InterestSummary -> InterestSummarySheet.newInstance(
                    state.selectedCryptoAccount!!,
                    state.selectedAsset!!
                )
                else -> null
            })
    }

    private fun showFiatFundsKyc(): BottomSheetDialogFragment {
        val currencyIcon = when (currencyPrefs.selectedFiatCurrency) {
            "EUR" -> R.drawable.ic_funds_euro
            "GBP" -> R.drawable.ic_funds_gbp
            else -> R.drawable.ic_funds_usd // show dollar if currency isn't selected
        }

        return KycBenefitsBottomSheet.newInstance(
            KycBenefitsBottomSheet.BenefitsDetails(
                title = getString(R.string.fiat_funds_no_kyc_announcement_title),
                description = getString(R.string.fiat_funds_no_kyc_announcement_description),
                listOfBenefits = listOf(
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_1_title),
                        getString(R.string.fiat_funds_no_kyc_step_1_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_2_title),
                        getString(R.string.fiat_funds_no_kyc_step_2_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_3_title),
                        getString(R.string.fiat_funds_no_kyc_step_3_description)
                    )
                ),
                icon = currencyIcon
            )
        )
    }

    private fun showAnnouncement(card: AnnouncementCard?) {
        displayList[IDX_CARD_ANNOUNCE] = card ?: EmptyDashboardItem()
        theAdapter.notifyItemChanged(IDX_CARD_ANNOUNCE)
    }

    private fun updateAnalytics(oldState: DashboardState?, newState: DashboardState) {
        analyticsReporter.updateFiatTotal(newState.fiatBalance)

        newState.assets.forEach { (cc, s) ->

            val newBalance = s.balance
            if (newBalance != null && newBalance != oldState?.assets?.get(cc)?.balance) {
                // If we have the full set, this will fire
                analyticsReporter.gotAssetBalance(cc, newBalance)
            }
        }
    }

    override fun onBackPressed(): Boolean = false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDashboardBinding =
        FragmentDashboardBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)

        setupSwipeRefresh()
        setupRecycler()

        if (flowToLaunch != null && flowCurrency != null) {
            compositeDisposable += coincore.fiatAssets.accountGroup().subscribeBy(
                onSuccess = { fiatGroup ->
                    val selectedAccount = fiatGroup.accounts.first {
                        (it as FiatAccount).fiatCurrency == flowCurrency
                    }

                    when (flowToLaunch) {
                        AssetAction.FiatDeposit -> model.process(
                            LaunchBankTransferFlow(
                                selectedAccount, AssetAction.FiatDeposit, false
                            )
                        )
                        AssetAction.Withdraw -> model.process(
                            LaunchBankTransferFlow(
                                selectedAccount, AssetAction.Withdraw, false
                            )
                        )
                        else -> throw IllegalStateException("Unsupported flow launch for action $flowToLaunch")
                    }
                },
                onError = {
                    // TODO
                }
            )
        }
    }

    private fun setupRecycler() {
        theLayoutManager = SafeLayoutManager(requireContext())

        binding.recyclerView.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter

            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }
        theAdapter.items = displayList
    }

    private fun setupToolbar() {
        activity.supportActionBar?.let {
            activity.setupToolbar(it, R.string.dashboard_title)
        }
    }

    private fun setupSwipeRefresh() {
        with(binding) {
            swipe.setOnRefreshListener { model.process(RefreshAllIntent) }

            // Configure the refreshing colors
            swipe.setColorSchemeResources(
                R.color.blue_800,
                R.color.blue_600,
                R.color.blue_400,
                R.color.blue_200
            )
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
        compositeDisposable += actionEvent.subscribe {
            initOrUpdateAssets()
        }

        (activity as? MainActivity)?.let {
            compositeDisposable += it.refreshAnnouncements.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (announcements.enable()) {
                        announcements.checkLatest(announcementHost, compositeDisposable)
                    }
                }
        }

        announcements.checkLatest(announcementHost, compositeDisposable)

        initOrUpdateAssets()
    }

    private fun initOrUpdateAssets() {
        if (displayList.isEmpty()) {
            model.process(GetAvailableAssets)
        } else {
            model.process(RefreshAllIntent)
        }
    }

    override fun onPause() {
        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, actionEvent)
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MainActivity.SETTINGS_EDIT,
            MainActivity.ACCOUNT_EDIT -> model.process(RefreshAllIntent)
            BACKUP_FUNDS_REQUEST_CODE -> {
                state?.backupSheetDetails?.let {
                    model.process(CheckBackupStatus(it.account, it.action))
                }
            }
            BankAuthActivity.LINK_BANK_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    state?.selectedFiatAccount?.let { fiatAccount ->
                        (state?.dashboardNavigationAction as? DashboardNavigationAction.LinkBankWithPartner)?.let {
                            model.process(
                                LaunchBankTransferFlow(
                                    fiatAccount,
                                    it.assetAction,
                                    true
                                )
                            )
                        }
                    }
                }
            }
        }

        model.process(ResetDashboardNavigation)
    }

    private fun onAssetClicked(cryptoCurrency: CryptoCurrency) {
        analytics.logEvent(assetActionEvent(AssetDetailsAnalytics.WALLET_DETAILS, cryptoCurrency.networkTicker))
        model.process(LaunchAssetDetailsFlow(cryptoCurrency))
    }

    private fun onFundsClicked(fiatAccount: FiatAccount) {
        analytics.logEvent(fiatAssetAction(AssetDetailsAnalytics.FIAT_DETAIL_CLICKED, fiatAccount.fiatCurrency))
        model.process(ShowFiatAssetDetails(fiatAccount))
    }

    private val announcementHost = object : AnnouncementHost {

        override val disposables: CompositeDisposable
            get() = compositeDisposable

        override fun showAnnouncementCard(card: AnnouncementCard) {
            model.process(ShowAnnouncement(card))
        }

        override fun dismissAnnouncementCard() {
            model.process(ClearAnnouncement)
        }

        override fun startKyc(campaignType: CampaignType) = navigator().launchKyc(campaignType)

        override fun startSwap() {
            analytics.logEvent(SwapAnalyticsEvents.SwapClickedEvent(LaunchOrigin.DASHBOARD_PROMO))
            navigator().tryTolaunchSwap()
        }

        override fun startPitLinking() = navigator().launchThePitLinking()

        override fun startFundsBackup() = navigator().launchBackupFunds()

        override fun startSetup2Fa() = navigator().launchSetup2Fa()

        override fun startVerifyEmail() = navigator().launchVerifyEmail()

        override fun startEnableFingerprintLogin() = navigator().launchSetupFingerprintLogin()

        override fun startIntroTourGuide() = navigator().launchIntroTour()

        override fun startTransferCrypto() = navigator().launchReceive()

        override fun startStxReceivedDetail() =
            model.process(ShowDashboardSheet(DashboardNavigationAction.StxAirdropComplete))

        override fun finishSimpleBuySignup() {
            navigator().resumeSimpleBuyKyc()
        }

        override fun startSimpleBuy(cryptoCurrency: CryptoCurrency) {
            navigator().startSimpleBuy(cryptoCurrency)
        }

        override fun startBuy() {
            analytics.logEvent(
                BuySellClicked(
                    origin = LaunchOrigin.DASHBOARD_PROMO, type = BuySellType.BUY
                )
            )
            navigator().launchSimpleBuySell()
        }

        override fun startSell() {
            analytics.logEvent(
                BuySellClicked(
                    origin = LaunchOrigin.DASHBOARD_PROMO, type = BuySellType.SELL
                )
            )
            navigator().launchSimpleBuySell(BuySellFragment.BuySellViewType.TYPE_SELL)
        }

        override fun startSend() {
            navigator().launchSend()
        }

        override fun startInterestDashboard() {
            navigator().startInterestDashboard()
        }

        override fun showFiatFundsKyc() {
            model.process(ShowDashboardSheet(DashboardNavigationAction.FiatFundsNoKyc))
        }

        override fun showBankLinking() =
            model.process(ShowBankLinkingSheet())

        override fun openBrowserLink(url: String) =
            requireContext().launchUrlInBrowser(url)
    }

    override fun onBankWireTransferSelected(currency: String) {
        state?.selectedFiatAccount?.let {
            model.process(ShowBankLinkingSheet(it))
        }
    }

    override fun startDepositFlow(fiatAccount: FiatAccount) {
        model.process(LaunchBankTransferFlow(fiatAccount, AssetAction.FiatDeposit, false))
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        state?.selectedFiatAccount?.let {
            if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit) {
                model.process(LaunchBankTransferFlow(it, AssetAction.FiatDeposit, true))
            } else if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw) {
                model.process(LaunchBankTransferFlow(it, AssetAction.Withdraw, true))
            }
        }
    }

    override fun startBankTransferWithdrawal(fiatAccount: FiatAccount) {
        model.process(LaunchBankTransferFlow(fiatAccount, AssetAction.Withdraw, false))
    }

    override fun showFundsKyc() {
        model.process(ShowDashboardSheet(DashboardNavigationAction.FiatFundsNoKyc))
    }

    override fun verificationCtaClicked() {
        navigator().launchKyc(CampaignType.FiatFunds)
    }

    // DialogBottomSheet.Host
    override fun onSheetClosed() {
        model.process(ClearBottomSheet)
    }

    override fun onFlowFinished() {
        model.process(ClearBottomSheet)
    }

    private fun launchNewSendFor(account: SingleAccount, action: AssetAction) {
        if (account is CustodialTradingAccount) {
            model.process(CheckBackupStatus(account, action))
        } else {
            model.process(LaunchSendFlow(account, action))
        }
    }

    override fun performAssetActionFor(action: AssetAction, account: BlockchainAccount) {
        clearBottomSheet()
        when (action) {
            AssetAction.Send -> launchNewSendFor(account as SingleAccount, action)
            else -> navigator().performAssetActionFor(action, account)
        }
    }

    override fun gotoActivityFor(account: BlockchainAccount) =
        navigator().performAssetActionFor(AssetAction.ViewActivity, account)

    override fun goToInterestDeposit(toAccount: InterestAccount) {
        model.process(LaunchInterestDepositFlow(toAccount))
    }

    override fun goToInterestWithdraw(fromAccount: InterestAccount) {
        model.process(LaunchInterestWithdrawFlow(fromAccount))
    }

    override fun goToSummary(account: SingleAccount, asset: CryptoCurrency) {
        model.process(UpdateSelectedCryptoAccount(account, asset))
        model.process(ShowDashboardSheet(DashboardNavigationAction.InterestSummary))
    }

    override fun goToSellFrom(account: CryptoAccount) {
        TransactionFlow(
            sourceAccount = account,
            action = AssetAction.Sell
        ).apply {
            startFlow(
                fragmentManager = childFragmentManager,
                host = this@DashboardFragment
            )
        }
    }

    override fun goToInterestDashboard() {
        navigator().startInterestDashboard()
    }

    override fun startBackupForTransfer() {
        navigator().launchBackupFunds(this, BACKUP_FUNDS_REQUEST_CODE)
    }

    override fun startTransferFunds(account: SingleAccount, action: AssetAction) {
        model.process(LaunchSendFlow(account, action))
    }

    companion object {
        fun newInstance() = DashboardFragment()

        fun newInstance(flowToLaunch: AssetAction, fiatCurrency: String) =
            DashboardFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(FLOW_TO_LAUNCH, flowToLaunch)
                    putString(FLOW_FIAT_CURRENCY, fiatCurrency)
                }
            }

        private const val FLOW_TO_LAUNCH = "FLOW_TO_LAUNCH"
        private const val FLOW_FIAT_CURRENCY = "FLOW_FIAT_CURRENCY"

        private const val IDX_CARD_ANNOUNCE = 0
        private const val IDX_CARD_BALANCE = 1
        private const val IDX_FUNDS_BALANCE = 2
        private const val IDX_ASSET_CARDS_START = 3

        private const val BACKUP_FUNDS_REQUEST_CODE = 8265
    }
}

/**
 * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
 */
private class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}
