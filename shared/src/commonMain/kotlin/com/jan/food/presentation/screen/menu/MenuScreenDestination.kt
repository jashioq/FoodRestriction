package com.jan.food.presentation.screen.menu

import kotlinx.serialization.Serializable

/**
 * @property selectedAllergenTags the allergen tags already selected when the screen opens, passed in
 * so the pills render in their final state immediately instead of flicking on after the first
 * use-case emission.
 */
@Serializable
data class MenuScreenDestination(
    val selectedAllergenTags: List<String> = emptyList(),
)
