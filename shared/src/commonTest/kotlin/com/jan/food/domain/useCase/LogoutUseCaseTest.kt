package com.jan.food.domain.useCase

import com.jan.food.domain.repository.AuthRepository
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LogoutUseCaseTest {
    private val authRepository = mock<AuthRepository>()
    private val useCase = LogoutUseCase(authRepository)

    @Test
    fun `call propagates repository success`() =
        runTest {
            everySuspend { authRepository.logout() } returns Result.success(Unit)

            assertTrue(useCase.call(Unit).isSuccess)
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { authRepository.logout() } returns Result.failure(RuntimeException("logout error"))

            assertTrue(useCase.call(Unit).isFailure)
        }
}
