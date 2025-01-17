package com.blockchain.core.interest.data.datasources

import com.blockchain.api.interest.InterestApiService
import com.blockchain.api.interest.data.InterestTickerLimitsDto
import com.blockchain.nabu.Authenticator
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.serializer
import timber.log.Timber

class InterestLimitsStore(
    private val authenticator: Authenticator,
    private val interestApiService: InterestApiService,
    private val currencyPrefs: CurrencyPrefs
) : Store<InterestTickerLimitsDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                authenticator.authenticate { token ->
                    interestApiService.getTickersLimits(
                        authHeader = token.authHeader,
                        fiatCurrencyTicker = currencyPrefs.selectedFiatCurrency.networkTicker
                    )
                }.doOnError { Timber.e("Limits call failed $it") }
            }
        ),
        dataSerializer = InterestTickerLimitsDto.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestLimitsStore"
    }
}
