package com.blockchain.api.dataremediation

import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query

interface DataRemediationApi {

    @GET("kyc/extra-questions")
    suspend fun getQuestionnaire(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("context") questionnaireContext: String
    ): Outcome<Exception, QuestionnaireResponse?>

    @PUT("kyc/extra-questions")
    suspend fun submitQuestionnaire(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body nodes: QuestionnaireResponse
    ): Outcome<Exception, Unit>
}
