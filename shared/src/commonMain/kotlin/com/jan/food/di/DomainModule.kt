package com.jan.food.di

import com.jan.food.domain.useCase.CheckProductUseCase
import com.jan.food.domain.useCase.EmitOnboardingFinishedUseCase
import com.jan.food.domain.useCase.EmitSessionUseCase
import com.jan.food.domain.useCase.LoginUseCase
import com.jan.food.domain.useCase.LogoutUseCase
import com.jan.food.domain.useCase.SetOnboardingFinishedUseCase
import org.koin.dsl.module

val domainModule = module {
    factory {
        SetOnboardingFinishedUseCase(
            dataStoreRepository = get(),
        )
    }

    factory {
        EmitOnboardingFinishedUseCase(
            dataStoreRepository = get(),
        )
    }

    factory {
        LoginUseCase(
            authRepository = get(),
        )
    }

    factory {
        LogoutUseCase(
            authRepository = get(),
        )
    }

    factory {
        EmitSessionUseCase(
            authRepository = get(),
        )
    }

    factory {
        CheckProductUseCase(
            foodRepository = get(),
        )
    }
}