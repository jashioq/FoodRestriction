package com.jan.food.data.repository

import app.cash.turbine.test
import com.jan.food.data.dataSource.auth.CognitoAuthClient
import com.jan.food.domain.repository.SecureStorageRepository
import com.jan.food.fixtures.authSession
import com.jan.food.fixtures.authenticationResult
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryTest {
    private val cognitoClient = mock<CognitoAuthClient>()
    private val secureStorage = mock<SecureStorageRepository>()
    private val json = Json { ignoreUnknownKeys = true }
    private val repository = AuthRepository(cognitoClient, secureStorage, json)

    // --- login ---

    @Test
    fun `login - success persists serialized session and returns success`() =
        runTest {
            everySuspend { cognitoClient.loginWithPassword(any(), any()) } returns Result.success(authenticationResult())
            everySuspend { secureStorage.putSecureString(any(), any()) } returns Result.success(Unit)

            assertTrue(repository.login("user@example.com", "password").isSuccess)
        }

    @Test
    fun `login - Cognito failure propagates as failure`() =
        runTest {
            everySuspend { cognitoClient.loginWithPassword(any(), any()) } returns Result.failure(RuntimeException("auth failed"))

            assertTrue(repository.login("user@example.com", "password").isFailure)
        }

    @Test
    fun `login - absent refresh token in Cognito response causes failure`() =
        runTest {
            everySuspend { cognitoClient.loginWithPassword(any(), any()) } returns
                Result.success(
                    authenticationResult(refreshToken = null),
                )

            assertTrue(repository.login("user@example.com", "password").isFailure)
        }

    // --- logout ---

    @Test
    fun `logout - delegates to clearSecureString and returns success`() =
        runTest {
            everySuspend { secureStorage.clearSecureString(any()) } returns Result.success(Unit)

            assertTrue(repository.logout().isSuccess)
        }

    @Test
    fun `logout - propagates clearSecureString failure`() =
        runTest {
            everySuspend { secureStorage.clearSecureString(any()) } returns Result.failure(RuntimeException("storage error"))

            assertTrue(repository.logout().isFailure)
        }

    // --- emitSession ---

    @Test
    fun `emitSession - blank stored value emits null`() =
        runTest {
            everySuspend { secureStorage.emitSecureString(any(), any()) } returns Result.success(MutableStateFlow(""))

            repository.emitSession().getOrThrow().test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitSession - valid stored JSON decodes to AuthSession (round-trip)`() =
        runTest {
            val session = authSession()
            everySuspend { secureStorage.emitSecureString(any(), any()) } returns
                Result.success(
                    MutableStateFlow(json.encodeToString(session)),
                )

            repository.emitSession().getOrThrow().test {
                assertEquals(session, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitSession - malformed stored value emits null`() =
        runTest {
            everySuspend { secureStorage.emitSecureString(any(), any()) } returns
                Result.success(
                    MutableStateFlow("{not-valid-json}"),
                )

            repository.emitSession().getOrThrow().test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // --- refreshSession ---

    @Test
    fun `refreshSession - carries existing refresh token forward when response omits one`() =
        runTest {
            val existing = authSession()
            everySuspend { secureStorage.emitSecureString(any(), any()) } returns
                Result.success(
                    MutableStateFlow(json.encodeToString(existing)),
                )
            everySuspend { cognitoClient.refresh(any()) } returns
                Result.success(
                    authenticationResult(idToken = "new-id", accessToken = "new-access", refreshToken = null),
                )
            everySuspend { secureStorage.putSecureString(any(), any()) } returns Result.success(Unit)

            val result = repository.refreshSession()

            assertTrue(result.isSuccess)
            val refreshed = result.getOrThrow()
            assertEquals("new-id", refreshed.idToken)
            assertEquals("new-access", refreshed.accessToken)
            assertEquals(existing.refreshToken, refreshed.refreshToken)
        }

    @Test
    fun `refreshSession - no session stored causes failure`() =
        runTest {
            everySuspend { secureStorage.emitSecureString(any(), any()) } returns Result.success(MutableStateFlow(""))

            assertTrue(repository.refreshSession().isFailure)
        }

    @Test
    fun `refreshSession - Cognito refresh failure propagates`() =
        runTest {
            val existing = authSession()
            everySuspend { secureStorage.emitSecureString(any(), any()) } returns
                Result.success(
                    MutableStateFlow(json.encodeToString(existing)),
                )
            everySuspend { cognitoClient.refresh(any()) } returns Result.failure(RuntimeException("refresh failed"))

            assertTrue(repository.refreshSession().isFailure)
        }
}
