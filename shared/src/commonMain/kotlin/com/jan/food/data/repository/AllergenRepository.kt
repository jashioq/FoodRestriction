package com.jan.food.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.jan.food.domain.model.Allergen
import com.jan.food.domain.model.SELECTED_ALLERGENS_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AllergenRepository(
    private val dataStore: DataStore<Preferences>,
) : com.jan.food.domain.repository.AllergenRepository {

    private val selectedKey = stringSetPreferencesKey(SELECTED_ALLERGENS_KEY)

    override suspend fun saveSelectedAllergens(allergens: List<Allergen>): Result<Unit> =
        runCatching {
            dataStore.edit { preferences ->
                preferences[selectedKey] = allergens.map { it.tag }.toSet()
            }
        }

    override suspend fun emitSelectedAllergens(): Result<Flow<List<Allergen>>> =
        runCatching {
            dataStore.data
                .map { preferences ->
                    preferences[selectedKey].orEmpty().mapNotNull(Allergen::fromTag)
                }
                .distinctUntilChanged()
        }

    override suspend fun getAvailableAllergens(): Result<List<Allergen>> =
        runCatching { Allergen.entries.toList() }
}
