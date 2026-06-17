package com.jan.food.di

import com.jan.food.domain.useCase.EmitOnboardingFinishedUseCase
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
}