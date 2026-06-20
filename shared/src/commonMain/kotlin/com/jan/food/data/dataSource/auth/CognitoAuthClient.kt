package com.jan.food.data.dataSource.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Thin client for the AWS Cognito `InitiateAuth` endpoint.
 *
 * Cognito speaks `application/x-amz-json-1.1`, which does not match Ktor's default JSON content
 * negotiation, so requests are encoded to a [String] with the injected [json] and responses are
 * read via [bodyAsText] and decoded manually. This client performs pure I/O — it does not persist
 * any tokens.
 *
 * @param httpClient a plain [HttpClient] without content negotiation.
 * @param json a lenient [Json] (`ignoreUnknownKeys = true`) used to (de)serialize Cognito payloads.
 * @param clientId the public Cognito app client id.
 */
class CognitoAuthClient(
    private val httpClient: HttpClient,
    private val json: Json,
    private val clientId: String,
) {
    /**
     * Authenticate a user with their username and password via the `USER_PASSWORD_AUTH` flow.
     * @param username the user's username (their email).
     * @param password the user's password.
     */
    suspend fun loginWithPassword(
        username: String,
        password: String,
    ): Result<AuthenticationResult> =
        initiateAuth(
            authFlow = "USER_PASSWORD_AUTH",
            authParameters = mapOf(
                "USERNAME" to username,
                "PASSWORD" to password,
            ),
        )

    /**
     * Exchange a refresh token for fresh tokens via the `REFRESH_TOKEN_AUTH` flow.
     * @param refreshToken the refresh token issued by a previous authentication.
     */
    suspend fun refresh(refreshToken: String): Result<AuthenticationResult> =
        initiateAuth(
            authFlow = "REFRESH_TOKEN_AUTH",
            authParameters = mapOf(
                "REFRESH_TOKEN" to refreshToken,
            ),
        )

    private suspend fun initiateAuth(
        authFlow: String,
        authParameters: Map<String, String>,
    ): Result<AuthenticationResult> = runCatching {
        val requestBody = json.encodeToString(
            InitiateAuthRequest(
                authFlow = authFlow,
                clientId = clientId,
                authParameters = authParameters,
            ),
        )

        val response = httpClient.post(COGNITO_URL) {
            contentType(COGNITO_CONTENT_TYPE)
            headers {
                append("X-Amz-Target", "$COGNITO_TARGET_PREFIX.InitiateAuth")
            }
            setBody(requestBody)
        }

        val responseBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Cognito InitiateAuth failed (${response.status.value}): $responseBody")
        }

        val initiateAuthResponse = json.decodeFromString<InitiateAuthResponse>(responseBody)
        initiateAuthResponse.authenticationResult
            ?: error("Cognito InitiateAuth returned no authentication result")
    }

    private companion object {
        const val COGNITO_URL = "https://cognito-idp.us-east-1.amazonaws.com/"
        const val COGNITO_TARGET_PREFIX = "AWSCognitoIdentityProviderService"
        val COGNITO_CONTENT_TYPE = ContentType("application", "x-amz-json-1.1")
    }
}
