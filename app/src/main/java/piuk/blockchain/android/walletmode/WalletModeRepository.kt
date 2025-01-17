package piuk.blockchain.android.walletmode

import android.content.SharedPreferences
import androidx.core.content.edit
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class WalletModeRepository(
    private val sharedPreferences: SharedPreferences,
    private val featureFlag: IntegratedFeatureFlag,
) : WalletModeService {

    override fun enabledWalletMode(): WalletMode {
        if (!featureFlag.isEnabled)
            return WalletMode.UNIVERSAL

        val walletModeString = sharedPreferences.getString(
            WALLET_MODE,
            ""
        )
        return WalletMode.values().firstOrNull { walletModeString == it.name } ?: defaultMode()
    }

    override fun reset() {
        _walletMode = MutableStateFlow(enabledWalletMode())
    }

    private fun defaultMode(): WalletMode =
        WalletMode.CUSTODIAL_ONLY

    private var _walletMode = MutableStateFlow(enabledWalletMode())

    override val walletMode: Flow<WalletMode>
        get() = _walletMode

    override fun updateEnabledWalletMode(type: WalletMode) {
        sharedPreferences.edit {
            putString(WALLET_MODE, type.name)
        }.also {
            _walletMode.value = type
        }
    }
}

private const val WALLET_MODE = "WALLET_MODE"
