package com.jan.food.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jan.food.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreRepository(
    private val dataStore: DataStore<Preferences>,
) : DataStoreRepository {
    override suspend fun putIntPreference(key: String, value: Int): Result<Unit> {
        val dataStoreKey = intPreferencesKey(key)
        return runCatching {
            dataStore.edit { preferences ->
                preferences[dataStoreKey] = value
            }
        }
    }

    override suspend fun emitIntPreference(key: String, default: Int): Result<Flow<Int>> {
        val dataStoreKey = intPreferencesKey(key)
        return runCatching {
            dataStore.data
                .map { preferences -> preferences[dataStoreKey] ?: default }
                .distinctUntilChanged()
        }
    }

    override suspend fun putStringPreference(key: String, value: String): Result<Unit> {
        val dataStoreKey = stringPreferencesKey(key)
        return runCatching {
            dataStore.edit { preferences ->
                preferences[dataStoreKey] = value
            }
        }
    }

    override suspend fun emitStringPreference(key: String, default: String): Result<Flow<String>> {
        val dataStoreKey = stringPreferencesKey(key)
        return runCatching {
            dataStore.data
                .map { preferences -> preferences[dataStoreKey] ?: default }
                .distinctUntilChanged()
        }
    }

    override suspend fun putBooleanPreference(key: String, value: Boolean): Result<Unit> {
        val dataStoreKey = booleanPreferencesKey(key)
        return runCatching {
            dataStore.edit { preferences ->
                preferences[dataStoreKey] = value
            }
        }
    }

    override suspend fun emitBooleanPreference(
        key: String,
        default: Boolean,
    ): Result<Flow<Boolean>> {
        val dataStoreKey = booleanPreferencesKey(key)
        return runCatching {
            dataStore.data
                .map { preferences -> preferences[dataStoreKey] ?: default }
                .distinctUntilChanged()
        }
    }

    override suspend fun putFloatPreference(key: String, value: Float): Result<Unit> {
        val dataStoreKey = floatPreferencesKey(key)
        return runCatching {
            dataStore.edit { preferences ->
                preferences[dataStoreKey] = value
            }
        }
    }

    override suspend fun emitFloatPreference(key: String, default: Float): Result<Flow<Float>> {
        val dataStoreKey = floatPreferencesKey(key)
        return runCatching {
            dataStore.data
                .map { preferences -> preferences[dataStoreKey] ?: default }
                .distinctUntilChanged()
        }
    }
}
