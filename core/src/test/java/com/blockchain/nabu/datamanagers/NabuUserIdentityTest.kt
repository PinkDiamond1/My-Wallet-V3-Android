package com.blockchain.nabu.datamanagers

import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycLimits
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierDetail
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.kyc.domain.model.TiersMap
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibleProduct
import com.blockchain.domain.eligibility.model.ProductEligibility
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.domain.eligibility.model.TransactionsLimit
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.Address
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibility
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NabuUserIdentityTest {

    private val custodialWalletManager: CustodialWalletManager = mock()
    private val interestService: InterestService = mock()
    private val simpleBuyEligibilityProvider: SimpleBuyEligibilityProvider = mock()
    private val kycService: KycService = mock()
    private val eligibilityService: EligibilityService = mock()
    private val userService: UserService = mock()
    private val bindFeatureFlag: FeatureFlag = mock()

    private val subject = NabuUserIdentity(
        custodialWalletManager = custodialWalletManager,
        interestService = interestService,
        simpleBuyEligibilityProvider = simpleBuyEligibilityProvider,
        kycService = kycService,
        eligibilityService = eligibilityService,
        userService = userService,
        bindFeatureFlag = bindFeatureFlag
    )

    @Test
    fun `on userAccessForFeature Buy should query eligibility data manager and simpleBuyTradingEligibility`() =
        runTest {
            val eligibility = ProductEligibility(
                product = EligibleProduct.BUY,
                canTransact = true,
                maxTransactionsCap = TransactionsLimit.Unlimited,
                reasonNotEligible = null
            )
            val mockTiers = createMockTiers(silverState = KycTierState.Verified, goldState = KycTierState.Verified)
            whenever(kycService.getTiersLegacy()).thenReturn(Single.just(mockTiers))
            whenever(eligibilityService.getProductEligibility(EligibleProduct.BUY))
                .thenReturn(Outcome.Success(eligibility))
            whenever(simpleBuyEligibilityProvider.simpleBuyTradingEligibility())
                .thenReturn(Single.just(SimpleBuyEligibility(true, true, 0, 1)))

            subject.userAccessForFeature(Feature.Buy)
                .test()
                .await()
                .assertValue(FeatureAccess.Granted())
        }

    @Test
    fun `on userAccessForFeature Swap should query eligibility data manager`() = runTest {
        val transactionsLimit = TransactionsLimit.Limited(3, 1)
        val eligibility = ProductEligibility(
            product = EligibleProduct.SWAP,
            canTransact = true,
            maxTransactionsCap = transactionsLimit,
            reasonNotEligible = null
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.SWAP))
            .thenReturn(Outcome.Success(eligibility))

        subject.userAccessForFeature(Feature.Swap)
            .test()
            .await()
            .assertValue(FeatureAccess.Granted(transactionsLimit))
    }

    @Test
    fun `on userAccessForFeature DepositCrypto should query eligibility data manager`() = runTest {
        val eligibility = ProductEligibility(
            product = EligibleProduct.DEPOSIT_CRYPTO,
            canTransact = false,
            maxTransactionsCap = TransactionsLimit.Unlimited,
            reasonNotEligible = ProductNotEligibleReason.InsufficientTier.Tier2Required
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.DEPOSIT_CRYPTO))
            .thenReturn(Outcome.Success(eligibility))

        subject.userAccessForFeature(Feature.DepositCrypto)
            .test()
            .await()
            .assertValue(FeatureAccess.Blocked(BlockedReason.InsufficientTier.Tier2Required))
    }

    @Test
    fun `on userAccessForFeature Sell should query eligibility data manager`() = runTest {
        val eligibility = ProductEligibility(
            product = EligibleProduct.SELL,
            canTransact = false,
            maxTransactionsCap = TransactionsLimit.Unlimited,
            reasonNotEligible = ProductNotEligibleReason.Sanctions.RussiaEU5
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.SELL))
            .thenReturn(Outcome.Success(eligibility))

        subject.userAccessForFeature(Feature.Sell)
            .test()
            .await()
            .assertValue(FeatureAccess.Blocked(BlockedReason.Sanctions.RussiaEU5))
    }

    @Test
    fun `on userAccessForFeature DepositFiat should query eligibility data manager`() = runTest {
        val eligibility = ProductEligibility(
            product = EligibleProduct.DEPOSIT_FIAT,
            canTransact = false,
            maxTransactionsCap = TransactionsLimit.Unlimited,
            reasonNotEligible = ProductNotEligibleReason.Sanctions.RussiaEU5
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.DEPOSIT_FIAT))
            .thenReturn(Outcome.Success(eligibility))

        subject.userAccessForFeature(Feature.DepositFiat)
            .test()
            .await()
            .assertValue(FeatureAccess.Blocked(BlockedReason.Sanctions.RussiaEU5))
    }

    @Test
    fun `on userAccessForFeature DepositInterest should query eligibility data manager`() = runTest {
        val eligibility = ProductEligibility(
            product = EligibleProduct.DEPOSIT_INTEREST,
            canTransact = false,
            maxTransactionsCap = TransactionsLimit.Unlimited,
            reasonNotEligible = ProductNotEligibleReason.Sanctions.RussiaEU5
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.DEPOSIT_INTEREST))
            .thenReturn(Outcome.Success(eligibility))

        subject.userAccessForFeature(Feature.DepositInterest)
            .test()
            .await()
            .assertValue(FeatureAccess.Blocked(BlockedReason.Sanctions.RussiaEU5))
    }

    @Test
    fun `on userAccessForFeature WithdrawFiat should query eligibility data manager`() = runTest {
        val eligibility = ProductEligibility(
            product = EligibleProduct.WITHDRAW_FIAT,
            canTransact = false,
            maxTransactionsCap = TransactionsLimit.Unlimited,
            reasonNotEligible = ProductNotEligibleReason.Sanctions.RussiaEU5
        )
        whenever(eligibilityService.getProductEligibility(EligibleProduct.WITHDRAW_FIAT))
            .thenReturn(Outcome.Success(eligibility))

        subject.userAccessForFeature(Feature.WithdrawFiat)
            .test()
            .await()
            .assertValue(FeatureAccess.Blocked(BlockedReason.Sanctions.RussiaEU5))
    }

    @Test
    fun `user is Argentinian`() {
        val mockAddress: Address = mock {
            on { countryCode }.thenReturn("AR")
        }
        val mockNabuUser: NabuUser = mock {
            on { address }.thenReturn(mockAddress)
        }
        whenever(userService.getUser()).thenReturn(Single.just(mockNabuUser))
        whenever(bindFeatureFlag.enabled).thenReturn(Single.just(true))

        subject.isArgentinian()
            .test()
            .assertValue(true)
    }

    @Test
    fun `user is not Argentinian`() {
        val mockAddress: Address = mock {
            on { countryCode }.thenReturn(null)
        }
        val mockNabuUser: NabuUser = mock {
            on { address }.thenReturn(mockAddress)
        }
        whenever(userService.getUser()).thenReturn(Single.just(mockNabuUser))
        whenever(bindFeatureFlag.enabled).thenReturn(Single.just(true))

        subject.isArgentinian()
            .test()
            .assertValue(false)
    }

    companion object {
        fun createMockTiers(silverState: KycTierState, goldState: KycTierState): KycTiers {
            return KycTiers(
                TiersMap(
                    mapOf(
                        KycTier.BRONZE to
                            KycTierDetail(
                                KycTierState.Verified,
                                KycLimits(null, null)
                            ),
                        KycTier.SILVER to
                            KycTierDetail(
                                silverState,
                                KycLimits(null, null)
                            ),
                        KycTier.GOLD to
                            KycTierDetail(
                                goldState,
                                KycLimits(null, null)
                            )
                    )
                )
            )
        }
    }
}
