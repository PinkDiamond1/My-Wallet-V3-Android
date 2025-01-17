package com.blockchain.defiwalletbackup.data.repository

import com.blockchain.defiwalletbackup.domain.errors.BackupPhraseError
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.BackupWallet
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class BackupPhraseServiceTest {
    private val payloadManager = mockk<PayloadDataManager>()
    private val backupWallet = mockk<BackupWallet>()
    private val walletStatusPrefs = mockk<WalletStatusPrefs>()

    private val backupPhraseService: BackupPhraseService = BackupPhraseRepository(
        payloadManager = payloadManager,
        backupWallet = backupWallet,
        walletStatusPrefs = walletStatusPrefs
    )

    private val mnemonic = listOf("A", "B")

    @Test
    fun `GIVEN phrase backed up, WHEN isBackedUp is called, THEN true should be returned`() {
        every { payloadManager.isBackedUp } returns true

        val result = backupPhraseService.isBackedUp()

        assertEquals(true, result)
    }

    @Test
    fun `GIVEN phrase not backed up, WHEN isBackedUp is called, THEN false should be returned`() {
        every { payloadManager.isBackedUp } returns false

        val result = backupPhraseService.isBackedUp()

        assertEquals(false, result)
    }

    @Test
    fun `GIVEN mnemonic exists, WHEN getMnemonic is called, THEN mnemonic should be returned`() {
        every { backupWallet.getMnemonic(any()) } returns mnemonic

        val result = backupPhraseService.getMnemonic(null)

        assertEquals(Outcome.Success(mnemonic), result)
    }

    @Test
    fun `GIVEN mnemonic null, WHEN getMnemonic is called, THEN Failure NoMnemonicFound should be returned`() {
        every { backupWallet.getMnemonic(any()) } returns null

        val result = backupPhraseService.getMnemonic(null)

        assertEquals(Outcome.Failure(BackupPhraseError.NoMnemonicFound), result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN sync successful, WHEN confirmRecoveryPhraseBackedUp is called, THEN Success should be returned`() =
        runTest {
            every { payloadManager.updateMnemonicVerified(true) } returns Completable.complete()
            every { walletStatusPrefs.lastBackupTime = any() } just Runs

            val result = backupPhraseService.confirmRecoveryPhraseBackedUp()

            assertEquals(Outcome.Success(Unit), result)
        }

    @Test
    fun `GIVEN sync unsuccessful, WHEN confirmRecoveryPhraseBackedUp is called, THEN Failure BackupConfirmationError should be returned`() =
        runTest {
            every { payloadManager.updateMnemonicVerified(true) } returns Completable.error(Exception())

            val result = backupPhraseService.confirmRecoveryPhraseBackedUp()

            assertEquals(Outcome.Failure(BackupPhraseError.BackupConfirmationError), result)
        }
}
