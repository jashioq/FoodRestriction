package com.jan.food.presentation.screen.onboarding

import com.jan.food.domain.util.UseCase
import com.jan.food.presentation.util.CoreViewModel
import com.jan.food.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * TBA
 */
class OnboardingScreenViewModel(
    private val setOnboardingFinishedUseCase: UseCase<Boolean, Unit>,
    scope: CoroutineScope? = null,
    logger: Logger? = null,
) : CoreViewModel<Unit, OnboardingScreenAction>(
    initialState = Unit,
    scope = scope,
    logger = logger,
) {
    override fun OnboardingScreenAction.process() {
        when (this@process) {
            is OnboardingScreenAction.Finish -> {
                vmScope.launch {
                    setOnboardingFinishedUseCase.call(true)
                }
            }
        }
    }
}
