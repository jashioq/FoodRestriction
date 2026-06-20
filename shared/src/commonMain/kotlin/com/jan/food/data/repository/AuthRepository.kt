package com.jan.food.data.repository

import com.jan.food.data.dataSource.auth.CognitoAuthClient
import com.jan.food.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class AuthRepository(
    private val cognitoAuthClient: CognitoAuthClient,
    private val secureStorageRepository: com.jan.food.domain.repository.SecureStorageRepository,
    private val json: Json,
) : com.jan.food.domain.repository.AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val authenticationResult = cognitoAuthClient.loginWithPassword(email, password).getOrThrow()
        val session = AuthSession(
            idToken = authenticationResult.idToken,
            accessToken = authenticationResult.accessToken,
            refreshToken = requireNotNull(authenticationResult.refreshToken) {
                "Cognito login returned no refresh token"
            },
        )
        secureStorageRepository.putSecureString(AUTH_SESSION_KEY, json.encodeToString(session))
            .getOrThrow()
    }

    override suspend fun logout(): Result<Unit> =
        secureStorageRepository.clearSecureString(AUTH_SESSION_KEY)

    override suspend fun emitSession(): Result<Flow<AuthSession?>> = runCatching {
        secureStorageRepository.emitSecureString(AUTH_SESSION_KEY, "")
            .getOrThrow()
            .map { stored -> stored.toSessionOrNull() }
    }

    override suspend fun refreshSession(): Result<AuthSession> = runCatching {
        val current = readSession()
            ?: error("Cannot refresh session: no session stored")
        val authenticationResult = cognitoAuthClient.refresh(current.refreshToken).getOrThrow()
        val session = AuthSession(
            idToken = authenticationResult.idToken,
            accessToken = authenticationResult.accessToken,
            // REFRESH_TOKEN_AUTH does not return a refresh token, so carry the existing one forward.
            refreshToken = authenticationResult.refreshToken ?: current.refreshToken,
        )
        secureStorageRepository.putSecureString(AUTH_SESSION_KEY, json.encodeToString(session))
            .getOrThrow()
        session
    }

    /**
     * One-shot read of the persisted session for internal data-layer needs (token refresh and the
     * authed client's bearer load). The public seam exposes only [emitSession]; this collapses that
     * same stream to its current value without leaking a one-shot read upward.
     */
    private suspend fun readSession(): AuthSession? =
        emitSession().getOrThrow().first()

    /** Decode a stored session string; blank (absent) or malformed values map to `null`. */
    private fun String.toSessionOrNull(): AuthSession? =
        if (isBlank()) null else runCatching { json.decodeFromString<AuthSession>(this) }.getOrNull()

    private companion object {
        const val AUTH_SESSION_KEY = "auth_session"
    }
}
