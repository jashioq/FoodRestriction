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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetOnboardingFinishedUseCaseTest {
    private val dataStoreRepository = mock<DataStoreRepository>()
    private val useCase = SetOnboardingFinishedUseCase(dataStoreRepository)

    @Test
    fun `call passes correct key and value through to repository`() =
        runTest {
            val capKey = Capture.slot<String>()
            val capValue = Capture.slot<Boolean>()
            everySuspend { dataStoreRepository.putBooleanPreference(capture(capKey, any()), capture(capValue, any())) } returns
                Result.success(Unit)

            useCase.call(true)

            assertEquals(ONBOARDING_FINISHED_KEY, capKey.get())
            assertEquals(true, capValue.get())
        }

    @Test
    fun `call propagates repository success`() =
        runTest {
            everySuspend { dataStoreRepository.putBooleanPreference(any(), any()) } returns Result.success(Unit)

            assertTrue(useCase.call(true).isSuccess)
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { dataStoreRepository.putBooleanPreference(any(), any()) } returns Result.failure(RuntimeException("write error"))

            assertTrue(useCase.call(false).isFailure)
        }
}
