package com.jan.food.presentation.screen.menu

import com.jan.food.domain.model.Allergen

sealed class MenuScreenAction {
    /**
     * Toggle [allergen] in the user's selection: add it if absent, remove it if present.
     * @param allergen the allergen whose selection should flip.
     */
    data class ToggleAllergen(val allergen: Allergen) : MenuScreenAction()
}
