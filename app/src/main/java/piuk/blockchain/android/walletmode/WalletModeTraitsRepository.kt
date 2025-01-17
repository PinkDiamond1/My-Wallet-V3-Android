package piuk.blockchain.android.walletmode

import com.blockchain.analytics.data.TraitsService
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService

class WalletModeTraitsRepository(private val walletModeService: Lazy<WalletModeService>) : TraitsService {
    override fun traits(): Map<String, String> {
        val walletMode = walletModeService.value.enabledWalletMode()
        return mapOf(
            "is_superapp_mvp" to (walletMode != WalletMode.UNIVERSAL).toString(),
            "app_mode" to walletMode.toTraitsString(),
        )
    }
}

private fun WalletMode.toTraitsString(): String {
    return when (this) {
        WalletMode.UNIVERSAL -> "UNIVERSAL"
        WalletMode.CUSTODIAL_ONLY -> "TRADING"
        WalletMode.NON_CUSTODIAL_ONLY -> "PKW"
    }
}
