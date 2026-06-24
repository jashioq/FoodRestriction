package com.jan.food.presentation.screen.menu

import com.jan.food.domain.model.Allergen

/**
 * @property selectedAllergens the allergens the user has currently toggled on.
 */
data class MenuScreenState(
    val selectedAllergens: Set<Allergen> = emptySet(),
)
