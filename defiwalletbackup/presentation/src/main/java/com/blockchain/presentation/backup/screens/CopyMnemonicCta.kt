package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Green600
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.CopyState
import com.blockchain.presentation.extensions.copyToClipboard

@Composable
fun CopyMnemonicCta(
    copyState: CopyState,
    mnemonic: List<String>,
    mnemonicCopied: () -> Unit
) {
    var copyMnemonic by remember { mutableStateOf(false) }

    if (copyMnemonic) {
        CopyMnemonic(mnemonic)
        mnemonicCopied()
        copyMnemonic = false
    }

    when (copyState) {
        is CopyState.Idle -> {
            TertiaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.common_copy),
                onClick = { copyMnemonic = true }
            )
        }

        CopyState.Copied -> {
            MnemonicCopied()
        }
    }
}

@Composable
fun MnemonicCopied() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.very_small_margin)),
        horizontalArrangement = Arrangement.Center
    ) {
        Image(imageResource = ImageResource.Local(R.drawable.ic_check))

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            text = stringResource(R.string.manual_backup_copied),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.body2,
            color = Green600
        )
    }
}

@Composable
fun CopyMnemonic(mnemonic: List<String>) {
    LocalContext.current.copyToClipboard(
        label = stringResource(id = R.string.manual_backup_title),
        text = mnemonic.joinToString(separator = " ")
    )
}
