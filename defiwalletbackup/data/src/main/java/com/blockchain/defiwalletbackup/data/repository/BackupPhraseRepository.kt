package com.blockchain.defiwalletbackup.data.repository

import com.blockchain.defiwalletbackup.domain.errors.BackupPhraseError
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.mapError
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.BackupWallet
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome
import timber.log.Timber

class BackupPhraseRepository(
    private val payloadManager: PayloadDataManager,
    private val backupWallet: BackupWallet,
    private val walletStatusPrefs: WalletStatusPrefs
) : BackupPhraseService {

    override fun isBackedUp() = payloadManager.isBackedUp

    override fun getMnemonic(secondPassword: String?): Outcome<BackupPhraseError, List<String>> {
        return backupWallet.getMnemonic(secondPassword)?.let {
            Outcome.Success(it)
        } ?: kotlin.run {
            Outcome.Failure(BackupPhraseError.NoMnemonicFound)
        }
    }

    override suspend fun confirmRecoveryPhraseBackedUp(): Outcome<BackupPhraseError, Unit> {
        return payloadManager.updateMnemonicVerified(true).awaitOutcome()
            .doOnSuccess {
                walletStatusPrefs.lastBackupTime = System.currentTimeMillis() / 1000
            }
            .mapError {
                Timber.e(it)
                BackupPhraseError.BackupConfirmationError
            }
    }
}
