package com.jan.food.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
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

class DataStoreRepositoryTest {
    private val dataStore = mock<DataStore<Preferences>>()
    private val repository = DataStoreRepository(dataStore)

    // --- Int ---

    @Test
    fun `putIntPreference writes value successfully`() =
        runTest {
            everySuspend { dataStore.updateData(any()) } returns mutablePreferencesOf()

            val result = repository.putIntPreference("count", 42)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `putIntPreference propagates DataStore write failure`() =
        runTest {
            everySuspend { dataStore.updateData(any()) } throws RuntimeException("write failed")

            val result = repository.putIntPreference("count", 1)

            assertTrue(result.isFailure)
        }

    @Test
    fun `emitIntPreference emits stored value`() =
        runTest {
            every { dataStore.data } returns flowOf(preferencesOf(intPreferencesKey("count") to 42))

            val result = repository.emitIntPreference("count", 0)

            assertTrue(result.isSuccess)
            result.getOrThrow().test {
                assertEquals(42, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitIntPreference emits default when key is absent`() =
        runTest {
            every { dataStore.data } returns flowOf(emptyPreferences())

            val result = repository.emitIntPreference("count", 99)

            result.getOrThrow().test {
                assertEquals(99, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitIntPreference suppresses duplicate values via distinctUntilChanged`() =
        runTest {
            val key = intPreferencesKey("count")
            every { dataStore.data } returns
                flowOf(
                    preferencesOf(key to 1),
                    preferencesOf(key to 1),
                    preferencesOf(key to 2),
                )

            val result = repository.emitIntPreference("count", 0)

            result.getOrThrow().test {
                assertEquals(1, awaitItem())
                assertEquals(2, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `emitIntPreference propagates DataStore read failure`() =
        runTest {
            every { dataStore.data } throws RuntimeException("read failed")

            val result = repository.emitIntPreference("count", 0)

            assertTrue(result.isFailure)
        }

    // --- String ---

    @Test
    fun `putStringPreference writes value successfully`() =
        runTest {
            everySuspend { dataStore.updateData(any()) } returns mutablePreferencesOf()

            val result = repository.putStringPreference("theme", "dark")

            assertTrue(result.isSuccess)
        }

    @Test
    fun `emitStringPreference emits stored value`() =
        runTest {
            every { dataStore.data } returns flowOf(preferencesOf(stringPreferencesKey("theme") to "dark"))

            val result = repository.emitStringPreference("theme", "light")

            result.getOrThrow().test {
                assertEquals("dark", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitStringPreference emits default when key is absent`() =
        runTest {
            every { dataStore.data } returns flowOf(emptyPreferences())

            val result = repository.emitStringPreference("theme", "light")

            result.getOrThrow().test {
                assertEquals("light", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // --- Boolean ---

    @Test
    fun `putBooleanPreference writes value successfully`() =
        runTest {
            everySuspend { dataStore.updateData(any()) } returns mutablePreferencesOf()

            val result = repository.putBooleanPreference("notifications", true)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `emitBooleanPreference emits stored value`() =
        runTest {
            every { dataStore.data } returns
                flowOf(
                    preferencesOf(booleanPreferencesKey("notifications") to false),
                )

            val result = repository.emitBooleanPreference("notifications", true)

            result.getOrThrow().test {
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitBooleanPreference emits default when key is absent`() =
        runTest {
            every { dataStore.data } returns flowOf(emptyPreferences())

            val result = repository.emitBooleanPreference("notifications", true)

            result.getOrThrow().test {
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // --- Float ---

    @Test
    fun `putFloatPreference writes value successfully`() =
        runTest {
            everySuspend { dataStore.updateData(any()) } returns mutablePreferencesOf()

            val result = repository.putFloatPreference("zoom", 1.5f)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `emitFloatPreference emits stored value`() =
        runTest {
            every { dataStore.data } returns flowOf(preferencesOf(floatPreferencesKey("zoom") to 2.0f))

            val result = repository.emitFloatPreference("zoom", 1.0f)

            result.getOrThrow().test {
                assertEquals(2.0f, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emitFloatPreference emits default when key is absent`() =
        runTest {
            every { dataStore.data } returns flowOf(emptyPreferences())

            val result = repository.emitFloatPreference("zoom", 1.0f)

            result.getOrThrow().test {
                assertEquals(1.0f, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
