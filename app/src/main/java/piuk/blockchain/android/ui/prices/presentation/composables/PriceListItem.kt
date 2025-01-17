package piuk.blockchain.android.ui.prices.presentation.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.walletconnect.R
import piuk.blockchain.android.ui.dashboard.asPercentString
import piuk.blockchain.android.ui.prices.presentation.PriceItemViewState

@Composable
fun PriceListItem(
    priceItem: PriceItemViewState,
    onClick: () -> Unit,
) {

    val accessAssetName = stringResource(piuk.blockchain.android.R.string.accessibility_asset_name)
    val accessCurrentMarketPrice =
        stringResource(piuk.blockchain.android.R.string.accessibility_current_market_price)
    val access24hChange = stringResource(piuk.blockchain.android.R.string.accessibility_24h_change)
    val accessPriceNotAvailable =
        stringResource(piuk.blockchain.android.R.string.accessibility_price_not_available)

    TableRow(
        contentStart = {
            Image(
                imageResource = ImageResource.Remote(
                    priceItem.assetInfo.logo
                ),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_margin))
                    .clip(CircleShape)
            )
        },

        content = {
            dimensionResource(com.blockchain.componentlib.R.dimen.medium_margin)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.medium_margin),
                        end = dimensionResource(
                            id = R.dimen.tiny_margin
                        )
                    )
            ) {
                Text(
                    text = priceItem.assetInfo.name,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title,
                    modifier = Modifier.semantics {
                        contentDescription = "$accessAssetName " + priceItem.assetInfo.name
                    }
                )
                Row {
                    Text(
                        modifier = Modifier.semantics {
                            this.contentDescription = "$accessCurrentMarketPrice " +
                                if (priceItem.hasError) {
                                    accessPriceNotAvailable
                                } else {
                                    priceItem.currentPrice
                                }
                        },
                        text = priceItem.currentPrice,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(id = R.dimen.tiny_margin))
                            .semantics {
                                this.contentDescription = "$access24hChange " +
                                    if (priceItem.hasError) {
                                        accessPriceNotAvailable
                                    } else {
                                        priceItem.delta
                                    }
                            },
                        text = priceItem.delta.takeIf { !it.isNullorNaN() }?.let {
                            it.asPercentString()
                        } ?: "--",
                        style = AppTheme.typography.paragraph1,
                        color = priceItem.delta.takeIf { !it.isNullorNaN() }?.let {
                            if (it >= 0) {
                                AppTheme.colors.success
                            } else {
                                AppTheme.colors.error
                            }
                        } ?: AppTheme.colors.body
                    )
                }
            }
        },
        contentEnd = {
            Image(
                imageResource = ImageResource.Local(R.drawable.ic_chevron_end),
                modifier = Modifier.requiredSizeIn(
                    maxWidth = dimensionResource(com.blockchain.componentlib.R.dimen.standard_margin),
                    maxHeight = dimensionResource(com.blockchain.componentlib.R.dimen.standard_margin),
                ),
            )
        },
        onContentClicked = onClick,
    )
    Divider(color = AppTheme.colors.light, thickness = 1.dp)
}

private fun Double?.isNullorNaN(): Boolean {
    return this == null || this.isNaN()
}
