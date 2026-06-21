package com.jan.food.domain.repository

import com.jan.food.domain.model.Allergen
import kotlinx.coroutines.flow.Flow

/**
 * Stores the user's selected allergens and exposes the set the backend can process.
 */
interface AllergenRepository {
    /**
     * Persist the user's allergen selection, replacing any previous selection.
     * @param allergens the allergens the user wants evaluated.
     */
    suspend fun saveSelectedAllergens(allergens: List<Allergen>): Result<Unit>

    /**
     * Emit the currently selected allergens, updating whenever the selection changes.
     */
    suspend fun emitSelectedAllergens(): Result<Flow<List<Allergen>>>

    /**
     * Read the full list of allergens the backend supports.
     */
    suspend fun getAvailableAllergens(): Result<List<Allergen>>
}
