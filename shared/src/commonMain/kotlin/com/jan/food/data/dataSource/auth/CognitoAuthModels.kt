package com.jan.food.data.dataSource.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for the Cognito `InitiateAuth` action.
 * @param authFlow the authentication flow, e.g. `USER_PASSWORD_AUTH` or `REFRESH_TOKEN_AUTH`.
 * @param clientId the public Cognito app client id.
 * @param authParameters the flow-specific parameters, e.g. `USERNAME`/`PASSWORD` or `REFRESH_TOKEN`.
 */
@Serializable
data class InitiateAuthRequest(
    @SerialName("AuthFlow") val authFlow: String,
    @SerialName("ClientId") val clientId: String,
    @SerialName("AuthParameters") val authParameters: Map<String, String>,
)

/**
 * Response body for the Cognito `InitiateAuth` action.
 * @param authenticationResult the issued tokens, present when authentication succeeds.
 * @param challengeName the name of a challenge to be answered, present when one is required.
 * @param session an opaque session token tied to a pending challenge.
 */
@Serializable
data class InitiateAuthResponse(
    @SerialName("AuthenticationResult") val authenticationResult: AuthenticationResult? = null,
    @SerialName("ChallengeName") val challengeName: String? = null,
    @SerialName("Session") val session: String? = null,
)

/**
 * The set of tokens issued by Cognito on a successful authentication.
 * @param accessToken the access token.
 * @param idToken the identity token; sent as the bearer to the app API.
 * @param refreshToken the refresh token; absent on a `REFRESH_TOKEN_AUTH` response.
 * @param expiresIn the access/identity token lifetime in seconds.
 * @param tokenType the token type, e.g. `Bearer`.
 */
@Serializable
data class AuthenticationResult(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("IdToken") val idToken: String,
    @SerialName("RefreshToken") val refreshToken: String? = null,
    @SerialName("ExpiresIn") val expiresIn: Int,
    @SerialName("TokenType") val tokenType: String,
)
