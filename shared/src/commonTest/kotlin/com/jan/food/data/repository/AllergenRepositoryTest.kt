package com.jan.food.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.cash.turbine.test
import com.jan.food.domain.model.Allergen
import com.jan.food.domain.model.SELECTED_ALLERGENS_KEY
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AllergenRepositoryTest {
    private val dataStore = mock<DataStore<Preferences>>()
    private val repository = AllergenRepository(dataStore)
    private val selectedKey = stringSetPreferencesKey(SELECTED_ALLERGENS_KEY)

    @Test
    fun `saveSelectedAllergens writes allergen tags successfully`() =
        runTest {
            everySuspend { dataStore.updateData(any()) } returns mutablePreferencesOf()

            val result = repository.saveSelectedAllergens(listOf(Allergen.MILK, Allergen.EGG))

            assertTrue(result.isSuccess)
        }

    @Test
    fun `saveSelectedAllergens propagates DataStore write failure`() =
        runTest {
            everySuspend { dataStore.updateData(any()) } throws RuntimeException("write failed")

            val result = repository.saveSelectedAllergens(listOf(Allergen.MILK))

            assertTrue(result.isFailure)
        }

    @Test
    fun `emitSelectedAllergens maps known tags and drops unknown ones`() =
        runTest {
            every { dataStore.data } returns
                flowOf(
                    preferencesOf(selectedKey to setOf("milk", "unknown_tag", "egg")),
                )

            val result = repository.emitSelectedAllergens()

            assertTrue(result.isSuccess)
            result.getOrThrow().test {
                val emitted = awaitItem()
                assertEquals(setOf(Allergen.MILK, Allergen.EGG), emitted.toSet())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitSelectedAllergens emits empty list when no allergens stored`() =
        runTest {
            every { dataStore.data } returns flowOf(preferencesOf())

            val result = repository.emitSelectedAllergens()

            assertTrue(result.isSuccess)
            result.getOrThrow().test {
                assertEquals(emptyList(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitSelectedAllergens propagates DataStore read failure`() =
        runTest {
            every { dataStore.data } throws RuntimeException("read failed")

            val result = repository.emitSelectedAllergens()

            assertTrue(result.isFailure)
        }

    @Test
    fun `getAvailableAllergens returns all Allergen entries`() =
        runTest {
            val result = repository.getAvailableAllergens()

            assertTrue(result.isSuccess)
            assertEquals(Allergen.entries.toList(), result.getOrThrow())
        }
}
