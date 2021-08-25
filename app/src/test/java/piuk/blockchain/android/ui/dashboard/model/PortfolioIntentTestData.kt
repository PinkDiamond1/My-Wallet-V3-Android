package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.Prices24HrWithDelta
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.mockito.Mock
import piuk.blockchain.android.coincore.AccountBalance
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

const val FIAT_CURRENCY = "USD"

val TEST_ASSETS = listOf(
    CryptoCurrency.BTC,
    CryptoCurrency.ETHER,
    CryptoCurrency.XLM
)

private val pricesWith24HrBtc = Prices24HrWithDelta(
    previousRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 400.toBigDecimal()),
    currentRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 400.toBigDecimal()),
    delta24h = 0.0
)

val initialBtcState = CryptoAssetState(
    currency = CryptoCurrency.BTC,
    accountBalance = AccountBalance(
        CryptoValue.zero(CryptoCurrency.BTC), CryptoValue.zero(CryptoCurrency.BTC),
        CryptoValue.zero(CryptoCurrency.BTC),
        ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 300.toBigDecimal())
    ),
    prices24HrWithDelta = pricesWith24HrBtc,
    priceTrend = emptyList()
)

val initialEthState = CryptoAssetState(
    currency = CryptoCurrency.ETHER,
    accountBalance = AccountBalance(
        CryptoValue.zero(CryptoCurrency.ETHER), CryptoValue.zero(CryptoCurrency.ETHER),
        CryptoValue.zero(CryptoCurrency.ETHER),
        ExchangeRate.CryptoToFiat(CryptoCurrency.ETHER, FIAT_CURRENCY, 200.toBigDecimal())
    ),
    prices24HrWithDelta = mock(),
    priceTrend = emptyList()
)

val initialXlmState = CryptoAssetState(
    currency = CryptoCurrency.XLM,
    accountBalance = AccountBalance(
        CryptoValue.zero(CryptoCurrency.XLM), CryptoValue.zero(CryptoCurrency.XLM),
        CryptoValue.zero(CryptoCurrency.XLM),
        ExchangeRate.CryptoToFiat(CryptoCurrency.XLM, FIAT_CURRENCY, 100.toBigDecimal())
    ),
    prices24HrWithDelta = mock(),
    priceTrend = emptyList()
)

val testAnnouncementCard_1 = StandardAnnouncementCard(
    name = "test_1",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val testAnnouncementCard_2 = StandardAnnouncementCard(
    name = "test_2",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val testBtcState = CryptoAssetState(
    currency = CryptoCurrency.BTC,
    accountBalance = AccountBalance(
        CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()),
        CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()),
        CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()),
        ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 300.toBigDecimal())
    ),
    prices24HrWithDelta = pricesWith24HrBtc,
    priceTrend = emptyList()
)

val testFiatBalance = FiatValue.fromMajor(FIAT_CURRENCY, 1000.toBigDecimal())

@Mock
private val fiatAccount: FiatAccount = mock()
val fiatAssetState_1 = FiatAssetState()
val fiatAssetState_2 = FiatAssetState(listOf(FiatBalanceInfo(testFiatBalance, testFiatBalance, fiatAccount)))

val initialState = PortfolioState(
    assets = mapOfAssets(
        CryptoCurrency.BTC to initialBtcState,
        CryptoCurrency.ETHER to initialEthState,
        CryptoCurrency.XLM to initialXlmState
    ),
    announcement = null
)