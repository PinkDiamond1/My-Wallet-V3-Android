package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinviewArgs(
    val networkTicker: String
) : ModelConfigArgs.ParcelableArgs {
    companion object {
        const val ARGS_KEY: String = "CoinviewArgs"
    }
}
