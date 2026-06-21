package com.jan.food.di

import com.jan.food.AppConfig
import com.jan.food.data.dataSource.auth.CognitoAuthClient
import com.jan.food.data.dataSource.food.FoodRemoteDataSource
import com.jan.food.data.repository.AllergenRepository
import com.jan.food.data.repository.AuthRepository
import com.jan.food.data.repository.DataStoreRepository
import com.jan.food.data.repository.FoodRepository
import com.jan.food.data.repository.SecureStorageRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val PLAIN_CLIENT = named("plainHttpClient")
private val AUTHED_CLIENT = named("authedHttpClient")
private val CLIENT_ID = named("cognitoClientId")
private val API_BASE_URL = named("apiBaseUrl")

val dataModule = module {
    single<com.jan.food.domain.repository.DataStoreRepository> {
        DataStoreRepository(dataStore = get())
    }
    single<com.jan.food.domain.repository.SecureStorageRepository> {
        SecureStorageRepository(secureStore = get())
    }
    single<com.jan.food.domain.repository.AllergenRepository> {
        AllergenRepository(dataStore = get())
    }

    // Lenient JSON, shared by the Cognito client and the authed app-API client.
    single<Json> {
        Json { ignoreUnknownKeys = true }
    }

    single(CLIENT_ID) { AppConfig.COGNITO_CLIENT_ID }
    single(API_BASE_URL) { AppConfig.API_BASE_URL }

    // Plain transport for Cognito: no content negotiation (payloads are encoded/decoded manually).
    single(PLAIN_CLIENT) {
        HttpClient {
            install(Logging)
        }
    }

    single {
        CognitoAuthClient(
            httpClient = get(PLAIN_CLIENT),
            json = get(),
            clientId = get(CLIENT_ID),
        )
    }

    single<com.jan.food.domain.repository.AuthRepository> {
        AuthRepository(
            cognitoAuthClient = get(),
            secureStorageRepository = get(),
            // Closed internal format — use the default Json, not the lenient Cognito one.
            json = Json,
        )
    }

    // Authenticated client for the app API: attaches the ID token as the bearer.
    single(AUTHED_CLIENT) {
        val authRepository = get<com.jan.food.domain.repository.AuthRepository>()
        val apiBaseUrl = get<String>(API_BASE_URL)
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(Logging)
            install(DefaultRequest) {
                url(apiBaseUrl)
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        authRepository.emitSession().getOrNull()?.first()?.let { session ->
                            BearerTokens(
                                accessToken = session.idToken,
                                refreshToken = session.refreshToken,
                            )
                        }
                    }
                    refreshTokens {
                        authRepository.refreshSession().getOrNull()?.let { session ->
                            BearerTokens(
                                accessToken = session.idToken,
                                refreshToken = session.refreshToken,
                            )
                        }
                    }
                }
            }
        }
    }

    single {
        FoodRemoteDataSource(httpClient = get(AUTHED_CLIENT))
    }

    single<com.jan.food.domain.repository.FoodRepository> {
        FoodRepository(foodRemoteDataSource = get())
    }
}
