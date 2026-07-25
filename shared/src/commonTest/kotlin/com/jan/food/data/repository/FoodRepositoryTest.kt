package com.jan.food.data.repository

import com.jan.food.data.dataSource.food.FoodRemoteDataSource
import com.jan.food.data.dataSource.food.RestrictionResult
import com.jan.food.fixtures.checkResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoodRepositoryTest {
    private val remoteDataSource = mock<FoodRemoteDataSource>()
    private val repository = FoodRepository(remoteDataSource)

    @Test
    fun `maps a successful CheckResponse to the domain ProductCheck`() =
        runTest {
            val response =
                checkResponse(
                    barcode = "1234567890128",
                    name = "Oat Bar",
                    source = "off",
                    found = true,
                    results =
                        listOf(
                            RestrictionResult(restriction = "gluten", status = "contains"),
                            RestrictionResult(restriction = "peanut", status = "absent"),
                        ),
                )
            everySuspend { remoteDataSource.checkProduct(any(), any()) } returns Result.success(response)

            val result = repository.checkProduct("1234567890128", listOf("gluten", "peanut"))

            assertTrue(result.isSuccess)
            val productCheck = result.getOrThrow()
            assertEquals("1234567890128", productCheck.barcode)
            assertEquals("Oat Bar", productCheck.name)
            assertEquals("off", productCheck.source)
            assertTrue(productCheck.found)
            assertEquals(2, productCheck.results.size)
            assertEquals("gluten", productCheck.results[0].restriction)
            assertEquals("contains", productCheck.results[0].status)
            assertEquals("peanut", productCheck.results[1].restriction)
            assertEquals("absent", productCheck.results[1].status)
        }

    @Test
    fun `propagates a data-source failure as Result failure`() =
        runTest {
            everySuspend {
                remoteDataSource.checkProduct(any(), any())
            } returns Result.failure(RuntimeException("network down"))

            val result = repository.checkProduct("1234567890128", listOf("gluten"))

            assertTrue(result.isFailure)
        }
}
