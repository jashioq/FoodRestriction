package com.jan.food.di

import org.koin.core.context.startKoin

actual class KoinInitializer {
    actual fun init() {
        startKoin {
            modules(presentationModule, domainModule, dataModule, platformDataSourceModule)
        }
    }
}