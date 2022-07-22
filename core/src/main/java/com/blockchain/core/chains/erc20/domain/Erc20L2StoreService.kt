package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.store.StoreRequest
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface Erc20L2StoreService {
    fun getBalances(
        networkTicker: String,
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(
        networkTicker: String,
        asset: AssetInfo,
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<Erc20Balance>

    fun getActiveAssets(
        networkTicker: String,
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Flow<Set<AssetInfo>>
}
