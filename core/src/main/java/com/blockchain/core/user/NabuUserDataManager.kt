package com.blockchain.core.user

import com.blockchain.api.services.ContactPreference
import com.blockchain.api.services.ContactPreferenceUpdate
import com.blockchain.api.services.NabuUserService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.kyc.domain.KycService
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface NabuUserDataManager {
    fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable

    fun getContactPreferences(): Single<List<ContactPreference>>

    fun updateContactPreferences(updates: List<ContactPreferenceUpdate>): Completable
}

class NabuUserDataManagerImpl(
    private val nabuUserService: NabuUserService,
    private val authenticator: AuthHeaderProvider,
    private val kycService: KycService,
) : NabuUserDataManager {

    override fun saveUserInitialLocation(countryIsoCode: String, stateIsoCode: String?): Completable =
        authenticator.getAuthHeader().map {
            nabuUserService.saveUserInitialLocation(
                it,
                countryIsoCode,
                stateIsoCode
            )
        }.flatMapCompletable { it }

    override fun getContactPreferences(): Single<List<ContactPreference>> =
        authenticator.getAuthHeader().flatMap {
            nabuUserService.getContactPreferences(it)
        }

    override fun updateContactPreferences(updates: List<ContactPreferenceUpdate>) =
        authenticator.getAuthHeader().flatMapCompletable {
            nabuUserService.updateContactPreferences(it, updates)
        }
}
