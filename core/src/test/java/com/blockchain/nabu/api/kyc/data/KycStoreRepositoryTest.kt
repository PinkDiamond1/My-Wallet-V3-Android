package com.blockchain.nabu.api.kyc.data

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.USD
import com.blockchain.nabu.api.kyc.data.store.KycStore
import com.blockchain.nabu.api.kyc.domain.KycStoreService
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Limits
import com.blockchain.nabu.models.responses.nabu.LimitsJson
import com.blockchain.nabu.models.responses.nabu.Tier
import com.blockchain.nabu.models.responses.nabu.TierResponse
import com.blockchain.nabu.models.responses.nabu.Tiers
import com.blockchain.nabu.models.responses.nabu.TiersResponse
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Money
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class KycStoreRepositoryTest {
    private val kycStore = mockk<KycStore>()
    private val assetCatalogue = mockk<AssetCatalogue>()

    private val kycStoreService: KycStoreService = KycStoreRepository(
        kycStore = kycStore,
        assetCatalogue = assetCatalogue
    )

    private val tiersResponse = TiersResponse(
        listOf(
            TierResponse(
                index = 0,
                name = "name",
                state = KycTierState.Verified,
                limits = null
            ),
            TierResponse(
                index = 1,
                name = "name",
                state = KycTierState.Pending,
                limits = LimitsJson(
                    currency = "USD",
                    daily = null,
                    annual = 1000.0.toBigDecimal()
                )
            ),
            TierResponse(
                index = 2,
                name = "name",
                state = KycTierState.None,
                limits = LimitsJson(
                    currency = "USD",
                    daily = 25000.0.toBigDecimal(),
                    annual = null
                )
            )
        )
    )

    private val kycTiers = KycTiers(
        Tiers(
            mapOf(
                KycTierLevel.BRONZE to
                    Tier(
                        KycTierState.Verified,
                        Limits(null, null)
                    ),
                KycTierLevel.SILVER to
                    Tier(
                        KycTierState.Pending,
                        Limits(null, Money.fromMajor(USD, 1000.0.toBigDecimal()))
                    ),
                KycTierLevel.GOLD to
                    Tier(
                        KycTierState.None,
                        Limits(
                            Money.fromMajor(USD, 25000.0.toBigDecimal()), null
                        )
                    )
            )
        )
    )

    @Before
    fun setUp() {
        every { kycStore.stream(any()) } returns
            flowOf(DataResource.Data(tiersResponse))

        every { assetCatalogue.fromNetworkTicker("USD") } returns USD
    }

    @Test
    fun `WHEN getKycTiers is called, THEN kycTiers should be returned`() {
        kycStoreService.getKycTiers(FreshnessStrategy.Cached(false))
            .test()
            .await()
            .assertValue {
                it == kycTiers
            }
    }
}
