package com.jan.food.domain.useCase

import com.jan.food.domain.repository.AuthRepository
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.matcher.capture.get
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginUseCaseTest {
    private val authRepository = mock<AuthRepository>()
    private val useCase = LoginUseCase(authRepository)

    @Test
    fun `call passes email and password through to repository`() =
        runTest {
            val capEmail = Capture.slot<String>()
            val capPass = Capture.slot<String>()
            everySuspend { authRepository.login(capture(capEmail, any()), capture(capPass, any())) } returns Result.success(Unit)

            useCase.call(LoginParams("user@example.com", "secret"))

            assertEquals("user@example.com", capEmail.get())
            assertEquals("secret", capPass.get())
        }

    @Test
    fun `call propagates repository success`() =
        runTest {
            everySuspend { authRepository.login(any(), any()) } returns Result.success(Unit)

            assertTrue(useCase.call(LoginParams("a@b.com", "pass")).isSuccess)
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { authRepository.login(any(), any()) } returns Result.failure(RuntimeException("auth error"))

            assertTrue(useCase.call(LoginParams("a@b.com", "pass")).isFailure)
        }
}
