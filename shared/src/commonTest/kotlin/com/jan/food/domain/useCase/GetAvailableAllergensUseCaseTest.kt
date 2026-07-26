package com.jan.food.domain.useCase

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.repository.AllergenRepository
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAvailableAllergensUseCaseTest {
    private val allergenRepository = mock<AllergenRepository>()
    private val useCase = GetAvailableAllergensUseCase(allergenRepository)

    @Test
    fun `call propagates repository success`() =
        runTest {
            val allergens = listOf(Allergen.MILK, Allergen.EGG)
            everySuspend { allergenRepository.getAvailableAllergens() } returns Result.success(allergens)

            val result = useCase.call(Unit)

            assertTrue(result.isSuccess)
            assertEquals(allergens, result.getOrThrow())
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { allergenRepository.getAvailableAllergens() } returns Result.failure(RuntimeException("read error"))

            assertTrue(useCase.call(Unit).isFailure)
        }
}
