package com.jan.food.presentation.screen.menu

import com.jan.food.presentation.util.CoreViewModel
import com.jan.food.util.Logger
import kotlinx.coroutines.CoroutineScope

/**
 * TBA
 */
class MenuScreenViewModel(
    scope: CoroutineScope? = null,
    logger: Logger? = null,
) : CoreViewModel<Unit, MenuScreenAction>(
    initialState = Unit,
    scope = scope,
    logger = logger,
) {
    override fun MenuScreenAction.process() {}
}
