package com.jan.food.domain.useCase

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.repository.AllergenRepository
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EmitSelectedAllergensUseCaseTest {
    private val allergenRepository = mock<AllergenRepository>()
    private val useCase = EmitSelectedAllergensUseCase(allergenRepository)

    @Test
    fun `call propagates repository success with the returned flow`() =
        runTest {
            val flow = MutableStateFlow<List<Allergen>>(emptyList())
            everySuspend { allergenRepository.emitSelectedAllergens() } returns Result.success(flow)

            val result = useCase.call(Unit)

            assertTrue(result.isSuccess)
            assertSame(flow, result.getOrThrow())
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { allergenRepository.emitSelectedAllergens() } returns Result.failure(RuntimeException("emit error"))

            assertTrue(useCase.call(Unit).isFailure)
        }
}
