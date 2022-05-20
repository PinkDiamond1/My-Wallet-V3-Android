package com.blockchain.api.services

import com.blockchain.api.referral.ReferralApi

class ReferralApiService(private val api: ReferralApi) {

    suspend fun getReferralCode(
        authorization: String,
        currency: String
    ) = api.getReferralCode(
        authorization = authorization,
        currency = currency,
        platform = WALLET
    )

    suspend fun validateReferralCode(
        authorization: String,
        referralCode: String
    ) = api.validateReferralCode(authorization, referralCode)
}

private const val WALLET = "wallet"
