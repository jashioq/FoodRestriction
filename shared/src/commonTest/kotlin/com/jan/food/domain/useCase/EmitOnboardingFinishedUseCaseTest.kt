package com.jan.food.domain.useCase

import com.jan.food.domain.model.ONBOARDING_FINISHED_KEY
import com.jan.food.domain.repository.DataStoreRepository
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.matcher.capture.get
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EmitOnboardingFinishedUseCaseTest {
    private val dataStoreRepository = mock<DataStoreRepository>()
    private val useCase = EmitOnboardingFinishedUseCase(dataStoreRepository)

    @Test
    fun `call passes correct key and default false to repository`() =
        runTest {
            val capKey = Capture.slot<String>()
            val capDefault = Capture.slot<Boolean>()
            everySuspend { dataStoreRepository.emitBooleanPreference(capture(capKey, any()), capture(capDefault, any())) } returns
                Result.success(MutableStateFlow(false))

            useCase.call(Unit)

            assertEquals(ONBOARDING_FINISHED_KEY, capKey.get())
            assertEquals(false, capDefault.get())
        }

    @Test
    fun `call propagates repository success with the returned flow`() =
        runTest {
            val flow = MutableStateFlow(false)
            everySuspend { dataStoreRepository.emitBooleanPreference(any(), any()) } returns Result.success(flow)

            val result = useCase.call(Unit)

            assertTrue(result.isSuccess)
            assertSame(flow, result.getOrThrow())
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { dataStoreRepository.emitBooleanPreference(any(), any()) } returns Result.failure(RuntimeException("emit error"))

            assertTrue(useCase.call(Unit).isFailure)
        }
}
