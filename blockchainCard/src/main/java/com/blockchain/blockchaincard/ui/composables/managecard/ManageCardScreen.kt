package com.blockchain.blockchaincard.ui.composables.managecard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionState
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionType
import com.blockchain.coincore.AccountBalance
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.DestructivePrimaryButton
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.DropdownMenuSearch
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.system.Webview
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.ToggleTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.UltraLight
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toFormattedDateTime
import com.blockchain.utils.toLocalTime
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

@Composable
fun ManageCard(
    card: BlockchainCard?,
    cardWidgetUrl: String?,
    linkedAccountBalance: AccountBalance?,
    isBalanceLoading: Boolean,
    isTransactionListRefreshing: Boolean,
    transactionList: List<BlockchainCardTransaction>?,
    onManageCardDetails: () -> Unit,
    onFundingAccountClicked: () -> Unit,
    onRefreshBalance: () -> Unit,
    onSeeTransactionDetails: (BlockchainCardTransaction) -> Unit,
    onRefreshTransactions: () -> Unit,
    onRefreshCardWidgetUrl: () -> Unit
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshBalance()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.paddingSmall)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.dimensions.paddingMedium
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SimpleText(
                    text = stringResource(R.string.my_card),
                    style = ComposeTypographies.Body2,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(1f))

                MinimalButton(
                    text = stringResource(id = R.string.manage_card),
                    onClick = onManageCardDetails,
                    modifier = Modifier
                        .wrapContentWidth()
                        .weight(1.4f),
                    minHeight = 16.dp,
                    shape = AppTheme.shapes.extraLarge
                )
            }

            when (cardWidgetUrl) {
                null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(
                            horizontal = AppTheme.dimensions.paddingMedium,
                            vertical = AppTheme.dimensions.xxxPaddingLarge
                        )
                    )
                }

                "" -> {
                    SimpleText(
                        text = stringResource(R.string.bc_card_unable_to_load_card),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Dark,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.paddingLarge)
                    )

                    SimpleText(
                        text = stringResource(R.string.bc_card_tap_here_to_try_again),
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Primary,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppTheme.dimensions.paddingSmall)
                            .clickable {
                                onRefreshCardWidgetUrl()
                            }
                    )
                }

                else -> {
                    Webview(
                        url = cardWidgetUrl,
                        disableScrolling = true,
                        modifier = Modifier
                            .padding(
                                top = AppTheme.dimensions.paddingMedium
                            )
                            .requiredHeight(355.dp)
                            .requiredWidth(200.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = UltraLight
            ) {
                if (linkedAccountBalance != null)
                    Column(modifier = Modifier.padding(AppTheme.dimensions.paddingMedium)) {
                        SimpleText(
                            text = stringResource(id = R.string.bc_card_transaction_payment_method),
                            style = ComposeTypographies.Paragraph1,
                            color = ComposeColors.Body,
                            gravity = ComposeGravities.Start
                        )

                        Spacer(modifier = Modifier.height(AppTheme.dimensions.paddingSmall))

                        FundingAccount(
                            accountBalance = linkedAccountBalance,
                            onFundingAccountClicked = onFundingAccountClicked,
                        )
                    }
                else if (isBalanceLoading)
                    ShimmerLoadingTableRow()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    AppTheme.dimensions.paddingMedium
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.dimensions.paddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                SimpleText(
                    text = stringResource(R.string.bc_card_transactions_title),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier.weight(1f)
                )

                // TODO (labreu): disabled for MVP
                /*Spacer(modifier = Modifier.weight(1f))

                MinimalButton(
                    text = stringResource(R.string.bc_card_see_all),
                    onClick = {},
                    modifier = Modifier
                        .wrapContentWidth()
                        .weight(1f),
                    minHeight = 16.dp,
                    shape = AppTheme.shapes.extraLarge
                )*/
            }

            when {
                transactionList == null -> {
                    ShimmerLoadingTableRow()
                }
                transactionList.isEmpty() -> {

                    Image(
                        painter = painterResource(id = R.drawable.empty_transactions_graphic),
                        contentDescription = stringResource(R.string.recent_purchases_here),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.paddingLarge),
                    )

                    SimpleText(
                        text = stringResource(R.string.bc_card_empty_transactions_title),
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.paddingSmall)
                    )

                    SimpleText(
                        text = stringResource(R.string.bc_card_empty_transactions_description),
                        style = ComposeTypographies.Paragraph1,
                        color = ComposeColors.Dark,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.paddingSmall)
                    )

                    SimpleText(
                        text = stringResource(R.string.bc_card_tap_here_to_refresh),
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Primary,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.paddingSmall, bottom = AppTheme.dimensions.paddingLarge)
                            .clickable {
                                onRefreshTransactions()
                            }
                    )
                }
                else -> {
                    CardTransactionList(
                        transactionList = transactionList,
                        onSeeTransactionDetails = onSeeTransactionDetails,
                        onRefreshTransactions = onRefreshTransactions,
                        isTransactionListRefreshing = isTransactionListRefreshing
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.paddingMedium))

            SimpleText(
                text = stringResource(R.string.bc_card_dashboard_legal_disclaimer),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Dark,
                gravity = ComposeGravities.Centre
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewManageCard() {
    ManageCard(
        card = null,
        cardWidgetUrl = null,
        linkedAccountBalance = null,
        isBalanceLoading = false,
        isTransactionListRefreshing = false,
        transactionList = null,
        onManageCardDetails = {},
        onFundingAccountClicked = {},
        onRefreshBalance = {},
        onSeeTransactionDetails = {},
        onRefreshTransactions = {},
        onRefreshCardWidgetUrl = {}
    )
}

@Composable
fun ManageCardDetails(
    last4digits: String,
    onToggleLockCard: (Boolean) -> Unit,
    cardStatus: BlockchainCardStatus,
    onSeePersonalDetails: () -> Unit,
    onSeeTransactionControls: () -> Unit,
    onSeeSupport: () -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {

        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.manage_card),
            shouldShowDivider = false
        )

        // Card details
        CardDetailsBottomSheetElement(
            cardStatus = cardStatus,
            last4digits = last4digits,
            modifier = Modifier.padding(
                AppTheme.dimensions.paddingLarge,
                AppTheme.dimensions.paddingMedium
            )
        )

        // Lock card
        ToggleTableRow(
            onCheckedChange = onToggleLockCard,
            isChecked = cardStatus == BlockchainCardStatus.LOCKED,
            primaryText = stringResource(R.string.lock_card),
            secondaryText = stringResource(R.string.temporarily_lock_card)
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        // Personal Details
        DefaultTableRow(
            primaryText = stringResource(R.string.personal_details),
            secondaryText = stringResource(R.string.view_account_information),
            onClick = onSeePersonalDetails,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        // Support
        DefaultTableRow(
            primaryText = stringResource(R.string.support),
            secondaryText = stringResource(R.string.get_help_with_card_issues),
            onClick = onSeeSupport,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewManageCardDetails() {
    ManageCardDetails("***3458", {}, BlockchainCardStatus.ACTIVE, {}, {}, {}, {})
}

@Composable
fun CardTransactionList(
    transactionList: List<BlockchainCardTransaction>,
    onSeeTransactionDetails: (transaction: BlockchainCardTransaction) -> Unit,
    onRefreshTransactions: () -> Unit,
    isTransactionListRefreshing: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Grey000),
        elevation = 0.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = isTransactionListRefreshing),
            onRefresh = onRefreshTransactions
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                transactionList.forEachIndexed { index, transaction ->
                    CardTransactionItem(
                        merchantName = transaction.merchantName,
                        timestamp = transaction.userTransactionTime,
                        amount = transaction.originalAmount.toStringWithSymbol(),
                        state = transaction.state,
                        isRefund = transaction.type == BlockchainCardTransactionType.REFUND,
                        onClick = { onSeeTransactionDetails(transaction) }
                    )
                    if (index < transactionList.lastIndex)
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewCardTransactionList() {

    val transactionList = listOf(
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.REFUND,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        )
    )

    CardTransactionList(
        transactionList = transactionList,
        onSeeTransactionDetails = {},
        onRefreshTransactions = { /*TODO*/ },
        isTransactionListRefreshing = false
    )
}

@Composable
fun CardTransactionItem(
    merchantName: String,
    timestamp: String,
    amount: String,
    state: BlockchainCardTransactionState,
    isRefund: Boolean,
    onClick: () -> Unit,
) {
    val transactionTitle: AnnotatedString
    val transactionAmount: AnnotatedString
    val transactionTimestamp: AnnotatedString?
    val transactionIcon: ImageResource

    val transactionTimestampFormatted = timestamp.fromIso8601ToUtc()?.toLocalTime()?.toFormattedDateTime()

    if (isRefund) {
        transactionTitle = buildAnnotatedString {
            append(stringResource(R.string.bc_card_transaction_refund_title, merchantName))
        }
        transactionAmount = buildAnnotatedString { append("+$amount") }
        transactionTimestamp = buildAnnotatedString { transactionTimestampFormatted?.let { append(it) } }
        transactionIcon = ImageResource.LocalWithBackground(
            R.drawable.ic_receive,
            backgroundColour = R.color.paletteBaseLight,
            iconTintColour = R.color.paletteBaseTextTitle,
            alpha = 1F
        )
    } else if (state == BlockchainCardTransactionState.DECLINED) {
        transactionTitle = buildAnnotatedString { append(merchantName) }
        transactionAmount = buildAnnotatedString {
            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append("+$amount")
            }
        }
        transactionTimestamp = buildAnnotatedString {
            withStyle(style = SpanStyle(color = AppTheme.colors.error)) {
                append(stringResource(id = R.string.bc_card_transaction_declined))
            }
        }
        transactionIcon = ImageResource.LocalWithBackground(
            R.drawable.ic_plus,
            backgroundColour = R.color.paletteBaseLight,
            iconTintColour = R.color.paletteBaseTextTitle,
            alpha = 1F
        )
    } else {
        transactionTitle = buildAnnotatedString { append(merchantName) }
        transactionAmount = buildAnnotatedString { append("-$amount") }
        transactionTimestamp = buildAnnotatedString { transactionTimestampFormatted?.let { append(it) } }
        transactionIcon = ImageResource.LocalWithBackground(
            R.drawable.ic_minus,
            backgroundColour = R.color.paletteBaseLight,
            iconTintColour = R.color.paletteBaseTextTitle,
            alpha = 1F
        )
    }

    DefaultTableRow(
        startImageResource = transactionIcon,
        primaryText = transactionTitle,
        secondaryText = transactionTimestamp,
        endText = transactionAmount,
        endImageResource = ImageResource.None,
        onClick = onClick,
    )
}

@Preview
@Composable
fun PreviewCardTransactionItem() {
    CardTransactionItem(
        merchantName = "Starbucks",
        timestamp = "2020-01-01T00:00:00.000Z",
        amount = "-$1.00",
        state = BlockchainCardTransactionState.COMPLETED,
        isRefund = false,
        onClick = { }
    )
}

@Composable
fun CardTransactionDetails(
    cardTransaction: BlockchainCardTransaction,
    last4digits: String,
    onCloseBottomSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val isRefund = cardTransaction.type == BlockchainCardTransactionType.REFUND

        val transactionAmount =
            when {
                isRefund -> {
                    buildAnnotatedString {
                        append("+${cardTransaction.fundingAmount.toStringWithSymbol()}")
                    }
                }
                cardTransaction.state == BlockchainCardTransactionState.DECLINED -> {
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append("-${cardTransaction.fundingAmount.toStringWithSymbol()}")
                        }
                    }
                }
                else -> {
                    buildAnnotatedString { append("-${cardTransaction.fundingAmount.toStringWithSymbol()}") }
                }
            }

        val merchantName = cardTransaction.merchantName

        val transactionDateTime =
            cardTransaction.userTransactionTime.fromIso8601ToUtc()?.toLocalTime()?.toFormattedDateTime() ?: ""

        val transactionStatus =
            if (cardTransaction.state == BlockchainCardTransactionState.DECLINED) {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = AppTheme.colors.error)) {
                        append(cardTransaction.state.toString())
                    }
                }
            } else {
                buildAnnotatedString {
                    append(cardTransaction.state.toString())
                }
            }

        val transactionPaymentMethod = cardTransaction.fundingAmount.currency.networkTicker

        val transactionFee = cardTransaction.fee.toStringWithSymbol()

        val originalTransactionAmount = cardTransaction.originalAmount.toStringWithSymbol()

        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.transaction_details_title),
            shouldShowDivider = true
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.paddingMedium))

        // Transaction Amount
        SimpleText(
            text = transactionAmount,
            style = ComposeTypographies.Title1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.paddingMedium))

        // Merchant Name
        SimpleText(
            text = merchantName,
            style = ComposeTypographies.Body2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start,
            modifier = Modifier
                .fillMaxWidth()
        )

        // Transaction timestamp
        SimpleText(
            text = transactionDateTime,
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.paddingMedium))

        // Transaction Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(AppTheme.dimensions.paddingMedium)
            ) {
                SimpleText(
                    text = stringResource(R.string.bc_card_transaction_status),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                )

                SimpleText(
                    text = transactionStatus,
                    style = ComposeTypographies.Body2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.paddingMedium))

        // Other Transaction details
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Card last 4 digits
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.paddingMedium)
                ) {
                    SimpleText(
                        text = stringResource(id = R.string.card),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    SimpleText(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = AppTheme.colors.primary)) {
                                append("•••• $last4digits")
                            }
                        },
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Payment Method
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.paddingMedium)
                ) {
                    SimpleText(
                        text = stringResource(R.string.bc_card_transaction_payment_method),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    SimpleText(
                        text = transactionPaymentMethod,
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Fee
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.paddingMedium)
                ) {
                    SimpleText(
                        text = stringResource(R.string.bc_card_transaction_fee),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    SimpleText(
                        text = transactionFee,
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Original transaction amount (total - fees)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.paddingMedium)
                ) {
                    SimpleText(
                        text = stringResource(
                            id = R.string.bc_card_transaction_original_amount,
                            transactionPaymentMethod
                        ),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    SimpleText(
                        text = originalTransactionAmount,
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCardTransactionDetails() {
    CardTransactionDetails(
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-06-21T12:00:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(0.20)),
            declineReason = null,
            networkConversionRate = null
        ),
        onCloseBottomSheet = {},
        last4digits = "1234"
    )
}

@Composable
fun TransactionControls(onCloseBottomSheet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.transaction_controls),
            shouldShowDivider = false
        )

        // Pin Settings
        SmallSectionHeader(text = stringResource(R.string.pin_settings), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.manage_pin),
            secondaryText = stringResource(R.string.manage_pin_description),
            onClick = {},
        )

        // Security Settings
        SmallSectionHeader(text = stringResource(R.string.security_settings), modifier = Modifier.fillMaxWidth())
        ToggleTableRow(
            primaryText = stringResource(R.string.swipe_payments),
            secondaryText = stringResource(R.string.swipe_payments_description),
            onCheckedChange = {}
        )
        ToggleTableRow(
            primaryText = stringResource(R.string.contactless_payments),
            secondaryText = stringResource(R.string.contactless_payments_description),
            onCheckedChange = {}
        )
        DefaultTableRow(
            primaryText = stringResource(R.string.transaction_amount_limit),
            secondaryText = stringResource(R.string.transaction_amount_limit_description),
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewTransactionControls() {
    TransactionControls({})
}

@Composable
fun PersonalDetails(
    shortAddress: String?,
    onCheckBillingAddress: () -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(id = R.string.personal_details),
            shouldShowDivider = false
        )

        if (!shortAddress.isNullOrEmpty()) {
            // Address
            DefaultTableRow(
                primaryText = stringResource(R.string.billing_address),
                secondaryText = shortAddress,
                onClick = onCheckBillingAddress,
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(
                        horizontal = AppTheme.dimensions.paddingMedium,
                        vertical = AppTheme.dimensions.xxxPaddingLarge
                    )
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewPersonalDetails() {
    PersonalDetails("614 Lorimer Street, Sacramento CA", {}) {}
}

@Composable
fun BillingAddress(
    address: BlockchainCardAddress,
    stateList: List<Region.State>?,
    onUpdateAddress: (BlockchainCardAddress) -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    // content
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.paddingMedium)
    ) {
        var addressLine1 by remember {
            mutableStateOf(address.line1)
        }
        var addressLine2 by remember {
            mutableStateOf(address.line2)
        }
        var city by remember {
            mutableStateOf(address.city)
        }
        var state by remember {
            mutableStateOf(address.state)
        }
        var postalCode by remember {
            mutableStateOf(address.postCode)
        }
        var country by remember {
            mutableStateOf("US") // TODO(labreu): design doesn't support other countries yet
        }
        var selectedState by remember {
            mutableStateOf(stateList?.find { it.stateCode == address.state }?.name ?: "")
        }
        val isStateValid = remember {
            mutableStateOf(false)
        }

        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.billing_address),
            shouldShowDivider = false
        )

        // Address line 1
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            text = stringResource(R.string.address_line_1),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = addressLine1,
            onValueChange = {
                addressLine1 = it
            },
            placeholder = { Text(stringResource(R.string.address_placeholder_1)) },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        // Address line 2
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            text = stringResource(R.string.address_line_2),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = addressLine2,
            onValueChange = {
                addressLine2 = it
            },
            placeholder = { Text(stringResource(R.string.address_placeholder_2)) },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        // City
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            text = stringResource(R.string.address_city),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = city,
            onValueChange = {
                city = it
            },
            placeholder = { Text(stringResource(R.string.address_city_placeholder)) },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        // State & Zip
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // State
            Column(modifier = Modifier.weight(1f)) {
                SimpleText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    text = stringResource(R.string.address_state),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )

                stateList?.map { it.name }?.let { stateNameList ->
                    DropdownMenuSearch(
                        value = TextFieldValue(selectedState),
                        onValueChange = {
                            selectedState = it.text
                        },
                        initialSuggestions = stateNameList.toMutableList(),
                    )
                }
            }

            Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

            // Postal code
            Column(modifier = Modifier.weight(1f)) {
                SimpleText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    text = stringResource(R.string.address_zip),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = {
                        postalCode = it
                    },
                    placeholder = { Text(stringResource(R.string.address_zip_placeholder)) },
                    singleLine = true,
                    textStyle = AppTheme.typography.body1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Grey000,
                        unfocusedBorderColor = Grey000
                    )
                )
            }
        }
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        val isFormValid = (
            addressLine1.isNotEmpty() &&
                city.isNotEmpty() &&
                state.isNotEmpty() &&
                postalCode.isNotEmpty() &&
                (stateList.isNullOrEmpty() || selectedState in stateList.map { it.name })
            )

        // Save
        PrimaryButton(
            text = stringResource(R.string.save),
            state = if (isFormValid) ButtonState.Enabled else ButtonState.Disabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_margin),
                    top = AppTheme.dimensions.paddingSmall,
                    end = dimensionResource(id = R.dimen.standard_margin),
                    bottom = dimensionResource(id = R.dimen.standard_margin)
                ),
            onClick = {
                onUpdateAddress(
                    BlockchainCardAddress(
                        line1 = addressLine1,
                        line2 = addressLine2,
                        postCode = postalCode,
                        city = city,
                        state = stateList?.find { it.name == selectedState }?.stateCode.orEmpty(),
                        country = country
                    )
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewBillingAddress() {
    BillingAddress(
        address = BlockchainCardAddress(
            line1 = "1242 Johnnyappleseed Ln.",
            line2 = "1234 Road Street",
            postCode = "94592",
            city = "Walnut Creek",
            state = "CA",
            country = "USA"
        ),
        stateList = emptyList(),
        onUpdateAddress = {},
        onCloseBottomSheet = {}
    )
}

@Composable
fun BillingAddressUpdated(
    success: Boolean,
    error: BlockchainCardError? = null,
    onDismiss: () -> Unit,
    onCloseBottomSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.billing_address),
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.xxxPaddingLarge))

        if (success) {
            BillingAddressUpdatedSuccess()
        } else {
            val errorTitle = when (error) {
                is BlockchainCardError.UXBlockchainCardError -> error.uxError.title
                else -> stringResource(R.string.address_update_failed)
            }

            val errorDescription = when (error) {
                is BlockchainCardError.UXBlockchainCardError -> error.uxError.description
                else -> stringResource(R.string.address_update_failed_description)
            }

            BillingAddressUpdatedFailed(errorTitle, errorDescription)
        }

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.xxxPaddingLarge))

        PrimaryButton(
            text = stringResource(id = R.string.common_confirm),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_margin),
                    top = AppTheme.dimensions.paddingSmall,
                    end = dimensionResource(id = R.dimen.standard_margin),
                    bottom = dimensionResource(id = R.dimen.standard_margin)
                ),
            onClick = onDismiss,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBillingAddressUpdated() {
    BillingAddressUpdated(success = true, null, onDismiss = {}, onCloseBottomSheet = {})
}

@Composable
fun BillingAddressUpdatedSuccess() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.address_updated_success),
            contentDescription = stringResource(R.string.address_updated),
            modifier = Modifier.wrapContentWidth(),
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.address_update_success),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.address_update_success_description),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBillingAddressUpdatedSuccess() {
    BillingAddressUpdatedSuccess()
}

@Composable
fun BillingAddressUpdatedFailed(errorTitle: String, errorDescription: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.ic_error),
            contentDescription = stringResource(R.string.address_updated),
            modifier = Modifier
                .wrapContentWidth()
                .size(74.dp),
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = errorTitle,
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingSmall))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = errorDescription,
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBillingAddressUpdatedError() {
    BillingAddressUpdatedFailed(errorTitle = "Error", errorDescription = "Something went wrong")
}

@Composable
fun Support(
    onCloseCard: () -> Unit,
    onClickCardLost: () -> Unit,
    onClickFAQ: () -> Unit,
    onClickContactSupport: () -> Unit,
    onCloseBottomSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(id = R.string.support),
            shouldShowDivider = false
        )

        // Support
        SmallSectionHeader(text = stringResource(R.string.support_title), modifier = Modifier.fillMaxWidth())

        // Card Lost
        DefaultTableRow(
            primaryText = stringResource(R.string.card_lost),
            secondaryText = stringResource(R.string.card_lost_description),
            onClick = onClickCardLost,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        // FAQ
        DefaultTableRow(
            primaryText = stringResource(R.string.visit_faq),
            secondaryText = stringResource(R.string.visit_faq_description),
            onClick = onClickFAQ,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        // Contact Support
        DefaultTableRow(
            primaryText = stringResource(R.string.contact_support),
            secondaryText = stringResource(R.string.contact_support_description),
            onClick = onClickContactSupport,
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingMedium))

        // Close card
        DestructivePrimaryButton(
            text = stringResource(id = R.string.terminate_card),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_margin),
                    top = AppTheme.dimensions.paddingSmall,
                    end = dimensionResource(id = R.dimen.standard_margin),
                    bottom = dimensionResource(id = R.dimen.standard_margin)
                ),
            onClick = onCloseCard,
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewSupport() {
    Support({}, {}, {}, {}, {})
}

@Composable
fun TerminateCard(last4digits: String, onConfirmCloseCard: () -> Unit, onCloseBottomSheet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(id = R.string.terminate_card),
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingLarge))

        Image(
            painter = painterResource(id = R.drawable.credit_card_failed),
            contentDescription = stringResource(R.string.card_created),
            modifier = Modifier.wrapContentWidth(),
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingMedium))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.terminate_card_number, last4digits),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.terminate_card_warning),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Muted,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingMedium))

        var closeCardConfirmationText by remember { mutableStateOf("") }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.terminate_card_confirm_description),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
            },
            value = closeCardConfirmationText,
            onValueChange = {
                closeCardConfirmationText = it
            },
            placeholder = {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.terminate_card_confirmation_text),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Muted,
                    gravity = ComposeGravities.Start
                )
            },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.paddingLarge))

        // Close card
        DestructivePrimaryButton(
            text = stringResource(id = R.string.terminate_card),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_margin),
                    top = AppTheme.dimensions.paddingSmall,
                    end = dimensionResource(id = R.dimen.standard_margin),
                    bottom = dimensionResource(id = R.dimen.standard_margin)
                ),
            onClick = onConfirmCloseCard,
            state = if (closeCardConfirmationText == stringResource(R.string.terminate_card_confirmation_text))
                ButtonState.Enabled
            else
                ButtonState.Disabled
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewCloseCard() {
    TerminateCard("1234", {}, {})
}

@Composable
private fun CardDetailsBottomSheetElement(
    cardStatus: BlockchainCardStatus,
    last4digits: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Grey100, RoundedCornerShape(8.dp))
            .background(UltraLight)
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.padding(horizontal = AppTheme.dimensions.paddingSmall)) {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.virtual_card),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )

                val cardStatusLabel =
                    if (cardStatus == BlockchainCardStatus.LOCKED) stringResource(R.string.bc_card_locked)
                    else stringResource(R.string.ready_to_use)

                val cardStatusColor =
                    if (cardStatus == BlockchainCardStatus.LOCKED) ComposeColors.Warning
                    else ComposeColors.Success

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = cardStatusLabel,
                    style = ComposeTypographies.Caption2,
                    color = cardStatusColor,
                    gravity = ComposeGravities.Start
                )
            }
        }

        SimpleText(
            text = "***$last4digits",
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingMedium)
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewCardDetailsBottomSheetElement() {
    CardDetailsBottomSheetElement(BlockchainCardStatus.ACTIVE, "***3458",)
}

@Composable
fun FundingAccountActionChooser(onAddFunds: () -> Unit, onChangeAsset: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = AppTheme.dimensions.paddingLarge)
            .fillMaxWidth()
    ) {

        SheetHeader(onClosePress = onClose, title = stringResource(R.string.select_one))

        Spacer(modifier = Modifier.padding(vertical = AppTheme.dimensions.paddingMedium))

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.bc_card_add_funds_title),
                secondaryText = stringResource(R.string.bc_card_add_funds_description),
                startImageResource = ImageResource.Local(
                    id = R.drawable.add_funds_icon,
                    contentDescription = stringResource(R.string.bc_card_add_funds_title)
                ),
                onClick = onAddFunds,
            )
        }

        Spacer(modifier = Modifier.padding(vertical = AppTheme.dimensions.xPaddingSmall))

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.bc_card_change_asset_title),
                secondaryText = stringResource(R.string.bc_card_change_asset_description),
                startImageResource = ImageResource.Local(
                    id = R.drawable.change_asset_icon,
                    contentDescription = stringResource(R.string.bc_card_change_asset_title),
                ),
                onClick = onChangeAsset,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFundingAccountActionChooser() {
    FundingAccountActionChooser({}, {}, {})
}

@Composable
fun AccountPicker(
    eligibleTradingAccountBalances: List<AccountBalance>,
    onAccountSelected: (String) -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Dark800
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        SimpleText(
            text = stringResource(R.string.spend_from),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingLarge)
        )

        AccountsContent(eligibleTradingAccountBalances, onAccountSelected)
    }
}

@Composable
fun AccountsContent(
    eligibleTradingAccountBalances: List<AccountBalance>,
    onAccountSelected: (String) -> Unit,
) {
    if (eligibleTradingAccountBalances.isNotEmpty()) {
        LazyColumn {
            itemsIndexed(
                items = eligibleTradingAccountBalances,
                itemContent = { index, balance ->
                    AccountItem(
                        accountBalance = balance,
                        onAccountSelected = onAccountSelected
                    )
                    if (index < eligibleTradingAccountBalances.lastIndex)
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                }
            )
        }
    } else {
        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.no_accounts_eligible_for_linking),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
}

@Composable
fun FundingAccount(accountBalance: AccountBalance, onFundingAccountClicked: () -> Unit) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Grey000),
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        when (accountBalance.total) {
            is FiatValue -> {
                FiatAccountItem(
                    currencyName = accountBalance.totalFiat.currency.name,
                    currencyTicker = accountBalance.totalFiat.currency.networkTicker,
                    currentBalance = accountBalance.totalFiat.toStringWithSymbol(),
                    currencyLogo = accountBalance.totalFiat.currency.logo,
                    endImageResource = ImageResource.Local(
                        id = R.drawable.ic_chevron_end,
                        contentDescription = null,
                    ),
                    onClick = onFundingAccountClicked
                )
            }
            is CryptoValue -> {
                CryptoAccountItem(
                    currencyName = accountBalance.total.currency.name,
                    currencyTicker = accountBalance.total.currency.networkTicker,
                    currentBalance = accountBalance.total.toStringWithSymbol(),
                    currentBalanceInFiat = accountBalance.totalFiat.toStringWithSymbol(),
                    currencyLogo = accountBalance.total.currency.logo,
                    endImageResource = ImageResource.Local(
                        id = R.drawable.ic_chevron_end,
                        contentDescription = null,
                    ),
                    onClick = onFundingAccountClicked
                )
            }
        }
    }
}

@Composable
fun AccountItem(accountBalance: AccountBalance, onAccountSelected: (String) -> Unit) {
    when (accountBalance.total) {
        is FiatValue -> {
            FiatAccountItem(
                currencyName = accountBalance.totalFiat.currency.name,
                currencyTicker = accountBalance.totalFiat.currency.networkTicker,
                currentBalance = accountBalance.totalFiat.toStringWithSymbol(),
                currencyLogo = accountBalance.totalFiat.currency.logo,
                onClick = { onAccountSelected(accountBalance.totalFiat.currency.networkTicker) }
            )
        }
        is CryptoValue -> {
            CryptoAccountItem(
                currencyName = accountBalance.total.currency.name,
                currencyTicker = accountBalance.total.currency.networkTicker,
                currentBalance = accountBalance.total.toStringWithSymbol(),
                currentBalanceInFiat = accountBalance.totalFiat.toStringWithSymbol(),
                currencyLogo = accountBalance.total.currency.logo,
                onClick = { onAccountSelected(accountBalance.total.currency.networkTicker) }
            )
        }
    }
}

@Composable
fun CryptoAccountItem(
    currencyName: String,
    currencyTicker: String,
    currentBalance: String,
    currentBalanceInFiat: String,
    currencyLogo: String,
    endImageResource: ImageResource = ImageResource.None,
    onClick: () -> Unit,
) {
    BalanceTableRow(
        titleStart = buildAnnotatedString { append(currencyName) },
        bodyStart = buildAnnotatedString { append(stringResource(id = R.string.custodial_wallet_default_label_1)) },
        titleEnd = buildAnnotatedString { append(currentBalance) },
        bodyEnd = buildAnnotatedString { append(currentBalanceInFiat) },
        startImageResource = ImageResource.Remote(
            url = currencyLogo,
            contentDescription = null,
        ),
        endImageResource = endImageResource,
        tags = emptyList(),
        onClick = onClick
    )
}

@Composable
fun FiatAccountItem(
    currencyName: String,
    currencyTicker: String,
    currentBalance: String,
    currencyLogo: String,
    endImageResource: ImageResource = ImageResource.None,
    onClick: () -> Unit,
) {
    BalanceTableRow(
        titleStart = buildAnnotatedString { append(currencyName) },
        titleEnd = buildAnnotatedString { append(currentBalance) },
        startImageResource = ImageResource.Remote(
            url = currencyLogo,
            contentDescription = null,
        ),
        endImageResource = endImageResource,
        tags = emptyList(),
        onClick = onClick
    )
}

@Composable
fun SupportPage() {
    // TODO (labreu): For now, all support pages point to FAQ page
    Webview(
        url = "https://www.blockchain.com/faq",
        modifier = Modifier
            .padding(top = AppTheme.dimensions.paddingMedium)
    )
}
