package com.jan.food.domain.useCase

import com.jan.food.domain.model.AuthSession
import com.jan.food.domain.repository.AuthRepository
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EmitSessionUseCaseTest {
    private val authRepository = mock<AuthRepository>()
    private val useCase = EmitSessionUseCase(authRepository)

    @Test
    fun `call propagates repository success with the returned flow`() =
        runTest {
            val flow = MutableStateFlow<AuthSession?>(null)
            everySuspend { authRepository.emitSession() } returns Result.success(flow)

            val result = useCase.call(Unit)

            assertTrue(result.isSuccess)
            assertSame(flow, result.getOrThrow())
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { authRepository.emitSession() } returns Result.failure(RuntimeException("emit error"))

            assertTrue(useCase.call(Unit).isFailure)
        }
}
