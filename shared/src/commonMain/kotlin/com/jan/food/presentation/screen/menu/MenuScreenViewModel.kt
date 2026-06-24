package com.jan.food.presentation.screen.menu

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.util.UseCase
import com.jan.food.presentation.util.CoreViewModel
import com.jan.food.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the allergen-selection grid: mirrors the persisted selection into [MenuScreenState] and
 * toggles individual allergens on tap.
 *
 * Selection is replace-based: a tap reads the current set, flips the tapped allergen, and saves the
 * whole list back via [saveSelectedAllergensUseCase]. The state is never mutated directly — it is
 * refreshed from [emitSelectedAllergensUseCase], the single source of truth.
 */
class MenuScreenViewModel(
    private val emitSelectedAllergensUseCase: UseCase<Unit, Flow<List<Allergen>>>,
    private val saveSelectedAllergensUseCase: UseCase<List<Allergen>, Unit>,
    scope: CoroutineScope? = null,
    logger: Logger? = null,
) : CoreViewModel<MenuScreenState, MenuScreenAction>(
    initialState = MenuScreenState(),
    scope = scope,
    logger = logger,
) {
    init {
        vmScope.launch {
            emitSelectedAllergensUseCase.call(Unit)
                .onSuccess { allergens ->
                    allergens.collect { selection ->
                        stateFlow.update { state ->
                            state.copy(selectedAllergens = selection.toSet())
                        }
                    }
                }
        }
    }

    override fun MenuScreenAction.process() {
        when (this@process) {
            is MenuScreenAction.ToggleAllergen -> {
                val current = stateFlow.value.selectedAllergens
                val updated = if (allergen in current) current - allergen else current + allergen
                vmScope.launch {
                    saveSelectedAllergensUseCase.call(updated.toList())
                }
            }
        }
    }
}
