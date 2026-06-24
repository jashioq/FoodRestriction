package com.jan.food.di

import com.jan.food.domain.useCase.CheckProductUseCase
import com.jan.food.domain.useCase.EmitOnboardingFinishedUseCase
import com.jan.food.domain.useCase.EmitSelectedAllergensUseCase
import com.jan.food.domain.useCase.EmitSessionUseCase
import com.jan.food.domain.useCase.LoginUseCase
import com.jan.food.domain.useCase.LogoutUseCase
import com.jan.food.domain.useCase.SaveSelectedAllergensUseCase
import com.jan.food.domain.useCase.SetOnboardingFinishedUseCase
import com.jan.food.navigation.NavigationViewModel
import com.jan.food.presentation.screen.home.HomeScreenViewModel
import com.jan.food.presentation.screen.menu.MenuScreenViewModel
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

    factory {
        HomeScreenViewModel(
            loginUseCase = get<LoginUseCase>(),
            logoutUseCase = get<LogoutUseCase>(),
            emitSessionUseCase = get<EmitSessionUseCase>(),
            checkProductUseCase = get<CheckProductUseCase>(),
            emitSelectedAllergensUseCase = get<EmitSelectedAllergensUseCase>(),
        )
    }

    factory { params ->
        MenuScreenViewModel(
            initialSelected = params.get(),
            emitSelectedAllergensUseCase = get<EmitSelectedAllergensUseCase>(),
            saveSelectedAllergensUseCase = get<SaveSelectedAllergensUseCase>(),
        )
    }
}