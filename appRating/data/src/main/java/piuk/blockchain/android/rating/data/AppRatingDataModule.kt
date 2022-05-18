package piuk.blockchain.android.rating.data

import com.blockchain.api.adapters.OutcomeCallAdapterFactory
import com.blockchain.koin.payloadScopeQualifier
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import piuk.blockchain.android.rating.data.api.AppRatingApi
import piuk.blockchain.android.rating.data.api.AppRatingApiConfig
import piuk.blockchain.android.rating.data.api.AppRatingEndpoints
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingApiKeysRemoteConfig
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.data.repository.AppRatingRepository
import piuk.blockchain.android.rating.domain.service.AppRatingService
import retrofit2.Retrofit

val checkMarketApiRetrofit = StringQualifier("kotlinx-api")

@OptIn(ExperimentalSerializationApi::class)
val appRatingDataModule = module {
    scope(payloadScopeQualifier) {
        scoped(checkMarketApiRetrofit) {
            val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

            Retrofit.Builder()
                .baseUrl(AppRatingApiConfig.URL)
                .client(get())
                .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        }

        factory {
            AppRatingApi(
                appRatingEndpoints = get<Retrofit>(checkMarketApiRetrofit).create(AppRatingEndpoints::class.java)
            )
        }

        scoped {
            AppRatingRemoteConfig(
                remoteConfig = get()
            )
        }

        scoped {
            AppRatingApiKeysRemoteConfig(
                remoteConfig = get(),
                json = get()
            )
        }

        scoped<AppRatingService> {
            AppRatingRepository(
                appRatingRemoteConfig = get(),
                appRatingApiKeysRemoteConfig = get(),
                defaultThreshold = DEFAULT_THRESHOLD,
                appRatingApi = get()
            )
        }

    }
}

private const val DEFAULT_THRESHOLD = 4
