package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.core.interest.domain.model.InterestLimits
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class InterestRepository(
    private val interestService: InterestService
) {
    private val limitsCache = TimedCacheRequest(
        cacheLifetimeSeconds = SHORT_LIFETIME,
        refreshFn = { interestService.getLimitsForAssets() }
    )

    private val availabilityCache = TimedCacheRequest(
        cacheLifetimeSeconds = LONG_LIFETIME,
        refreshFn = { interestService.getAllAvailableAssets() }
    )

    private val eligibilityCache = TimedCacheRequest(
        cacheLifetimeSeconds = LONG_LIFETIME,
        refreshFn = { interestService.getEligibilityForAssets() }
    )

    fun getLimitForAsset(asset: AssetInfo): Single<InterestLimits> =
        limitsCache.getCachedSingle().map { mapAssettWithLimits ->
            mapAssettWithLimits[asset]
                ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }

    fun getAvailabilityForAsset(ccy: AssetInfo): Single<Boolean> =
        availabilityCache.getCachedSingle().flatMap { enabledList ->
            Single.just(enabledList.contains(ccy))
        }.onErrorResumeNext { Single.just(false) }

    fun getAvailableAssets(): Single<List<AssetInfo>> =
        availabilityCache.getCachedSingle()

    fun getEligibilityForAsset(assetInfo: AssetInfo): Single<InterestEligibility> =
        eligibilityCache.getCachedSingle().map { mapAssetWithEligibility ->
            mapAssetWithEligibility[assetInfo] ?: InterestEligibility.Ineligible.default()
        }

    companion object {
        private const val SHORT_LIFETIME = 240L
        private const val LONG_LIFETIME = 3600L
    }
}
