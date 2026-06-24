package com.jan.food.di

import com.jan.food.data.dataSource.IosSecureStore
import com.jan.food.data.dataSource.SecureStore
import com.jan.food.data.dataSource.createIosDataStore
import com.jan.food.presentation.util.Haptic
import org.koin.dsl.module

actual val platformModule = module {
    single { createIosDataStore() }
    single<SecureStore> { IosSecureStore() }
    // Eager so the Taptic Engine is warmed at startup, not on the first impact.
    single(createdAtStart = true) { Haptic() }
}