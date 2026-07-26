package com.jan.food.domain.useCase

import com.jan.food.domain.model.ProductCheck
import com.jan.food.domain.repository.FoodRepository
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

class CheckProductUseCaseTest {
    private val foodRepository = mock<FoodRepository>()
    private val useCase = CheckProductUseCase(foodRepository)

    private val productCheck =
        ProductCheck(
            barcode = "5901234123457",
            name = "Test Cookies",
            source = "usda",
            found = true,
            results = emptyList(),
        )

    @Test
    fun `call passes barcode and restrictions through to repository`() =
        runTest {
            val capBarcode = Capture.slot<String>()
            val capRestrictions = Capture.slot<List<String>>()
            everySuspend { foodRepository.checkProduct(capture(capBarcode, any()), capture(capRestrictions, any())) } returns
                Result.success(productCheck)

            useCase.call(CheckProductParams("5901234123457", listOf("gluten", "milk")))

            assertEquals("5901234123457", capBarcode.get())
            assertEquals(listOf("gluten", "milk"), capRestrictions.get())
        }

    @Test
    fun `call propagates repository success`() =
        runTest {
            everySuspend { foodRepository.checkProduct(any(), any()) } returns Result.success(productCheck)

            assertTrue(useCase.call(CheckProductParams("123", listOf("gluten"))).isSuccess)
        }

    @Test
    fun `call propagates repository failure`() =
        runTest {
            everySuspend { foodRepository.checkProduct(any(), any()) } returns Result.failure(RuntimeException("not found"))

            assertTrue(useCase.call(CheckProductParams("123", listOf("gluten"))).isFailure)
        }
}
