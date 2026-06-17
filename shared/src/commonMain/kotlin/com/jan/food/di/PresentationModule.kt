package com.jan.food.di

import com.jan.food.domain.useCase.EmitOnboardingFinishedUseCase
import com.jan.food.domain.useCase.SetOnboardingFinishedUseCase
import com.jan.food.navigation.NavigationViewModel
import com.jan.food.presentation.screen.onboarding.OnboardingScreenViewModel
import org.koin.dsl.module

val presentationModule = module {
    factory {
        NavigationViewModel(
            emitOnboardingFinishedUseCase = get<EmitOnboardingFinishedUseCase>(),
        )
    }

    factory {
        OnboardingScreenViewModel(
            setOnboardingFinishedUseCase = get<SetOnboardingFinishedUseCase>(),
        )
    }
}