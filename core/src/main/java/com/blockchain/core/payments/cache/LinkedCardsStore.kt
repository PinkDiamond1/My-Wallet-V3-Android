package com.blockchain.core.payments.cache

import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.builtins.ListSerializer

class LinkedCardsStore(
    private val paymentMethodsService: PaymentMethodsService,
    private val authenticator: Authenticator
) : Store<List<CardResponse>> by PersistedJsonSqlDelightStoreBuilder().build(
    storeId = STORE_ID,
    fetcher = Fetcher.ofSingle(
        mapper = {
            authenticator.getAuthHeader()
                .flatMap { paymentMethodsService.getCards(it, true) }
        },
    ),
    dataSerializer = ListSerializer(CardResponse.serializer()),
    mediator = FreshnessMediator(Freshness.ofMinutes(5L))
) {
    companion object {
        private const val STORE_ID = "LinkedCardsStore"
    }
}
