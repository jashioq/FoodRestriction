package com.jan.food.navigation

import com.jan.food.domain.model.OnboardingState
import com.jan.food.domain.util.UseCase
import com.jan.food.presentation.util.CoreViewModel
import com.jan.food.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * View model used for handling onboarding state. It controls which NavHost should be used.
 * @param emitOnboardingFinishedUseCase a [UseCase] used for emitting onboarding finished status.
 * @see MainNavHost
 * @see OnboardingNavHost
 */
class NavigationViewModel(
    private val emitOnboardingFinishedUseCase: UseCase<Unit, Flow<Boolean>>,
    scope: CoroutineScope? = null,
    logger: Logger? = null,
) : CoreViewModel<OnboardingState, Unit>(
    initialState = OnboardingState.LOADING,
    scope = scope,
    logger = logger,
) {
    init {
        vmScope.launch {
            emitOnboardingFinishedUseCase.call(value = Unit).onSuccess {
                it.collect { finished ->
                    stateFlow.update {
                        if (finished) {
                            OnboardingState.FINISHED
                        } else {
                            OnboardingState.NOT_FINISHED
                        }
                    }
                }
            }
        }
    }

    override fun Unit.process() {}
}
