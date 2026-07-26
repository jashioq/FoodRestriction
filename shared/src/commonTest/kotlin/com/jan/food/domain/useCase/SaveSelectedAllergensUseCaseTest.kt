package com.jan.food.domain.useCase

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.repository.AllergenRepository
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

class SaveSelectedAllergensUseCaseTest {
    private val allergenRepository = mock<AllergenRepository>()
    private val useCase = SaveSelectedAllergensUseCase(allergenRepository)

    @Test
    fun `call passes allergen list through to repository`() =
        runTest {
            val cap = Capture.slot<List<Allergen>>()
            everySuspend { allergenRepository.saveSelectedAllergens(capture(cap, any())) } returns Result.success(Unit)

            useCase.call(listOf(Allergen.MILK, Allergen.GLUTEN))

            assertEquals(listOf(Allergen.MILK, Allergen.GLUTEN), cap.get())
        }

    @Test
    fun `call propagates repository success`() =
        runTest {
            everySuspend { allergenRepository.saveSelectedAllergens(any()) } returns Result.success(Unit)

            assertTrue(useCase.call(listOf(Allergen.EGG)).isSuccess)
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { allergenRepository.saveSelectedAllergens(any()) } returns Result.failure(RuntimeException("write error"))

            assertTrue(useCase.call(listOf(Allergen.EGG)).isFailure)
        }
}
