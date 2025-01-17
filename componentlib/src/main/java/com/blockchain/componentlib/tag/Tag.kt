package com.blockchain.componentlib.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Grey000

@Composable
fun Tag(
    text: String,
    size: TagSize,
    defaultBackgroundColor: Color,
    defaultTextColor: Color,
    borders: Boolean = false,
    onClick: (() -> Unit)?
) {

    val paddingHorizontal = when (size) {
        TagSize.Primary -> 8.dp
        TagSize.Large -> dimensionResource(R.dimen.very_small_margin)
    }

    val paddingVertical = when (size) {
        TagSize.Primary -> dimensionResource(R.dimen.smallest_margin)
        TagSize.Large -> dimensionResource(R.dimen.minuscule_margin)
    }

    val textStyle = when (size) {
        TagSize.Primary -> AppTheme.typography.caption2
        TagSize.Large -> AppTheme.typography.paragraph2
    }

    onClick?.let { action ->
        Row(
            modifier = Modifier
                .border(
                    width = if (borders) 1.dp else 0.dp,
                    color = Grey000,
                    shape = RoundedCornerShape(size = dimensionResource(R.dimen.smallest_margin)),
                )
                .clip(RoundedCornerShape(size = dimensionResource(R.dimen.smallest_margin)))
                .background(defaultBackgroundColor)
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
                .clickable(onClick = action),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                imageResource = ImageResource.Local(
                    id = R.drawable.ic_info_outline,
                    colorFilter = ColorFilter.tint(defaultTextColor, blendMode = BlendMode.SrcAtop)
                )
            )

            Spacer(modifier = Modifier.width(width = dimensionResource(R.dimen.minuscule_margin)))

            Text(
                text = text,
                style = textStyle,
                color = defaultTextColor
            )

            Spacer(modifier = Modifier.width(width = dimensionResource(R.dimen.tiny_margin)))

            Image(
                imageResource = ImageResource.Local(
                    id = R.drawable.ic_chevron_end_small,
                    colorFilter = ColorFilter.tint(defaultTextColor, blendMode = BlendMode.SrcAtop)
                )
            )
        }
    } ?: run {
        Text(
            text = text,
            style = textStyle,
            color = defaultTextColor,
            modifier = Modifier
                .border(
                    width = if (borders) 1.dp else 0.dp,
                    color = Grey000,
                    shape = RoundedCornerShape(size = dimensionResource(R.dimen.smallest_margin)),
                )
                .clip(RoundedCornerShape(size = dimensionResource(R.dimen.smallest_margin)))
                .background(defaultBackgroundColor)
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
        )
    }
}

@Preview
@Composable
fun ClickableTag() {
    AppTheme(darkTheme = false) {
        AppSurface {
            Tag(
                text = "Click me",
                size = TagSize.Primary,
                defaultBackgroundColor = Dark600,
                defaultTextColor = Blue400,
                borders = true,
                onClick = { },
            )
        }
    }
}

@Preview
@Composable
fun NonClickableTag() {
    AppTheme(darkTheme = false) {
        AppSurface {
            Tag(
                text = "Click me",
                size = TagSize.Primary,
                defaultBackgroundColor = Dark600,
                defaultTextColor = Blue400,
                borders = true,
                onClick = null,
            )
        }
    }
}
