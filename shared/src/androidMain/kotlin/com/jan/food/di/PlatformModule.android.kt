package com.jan.food.di

import com.jan.food.data.dataSource.AndroidSecureStore
import com.jan.food.data.dataSource.SecureStore
import com.jan.food.data.dataSource.createAndroidDataStore
import com.jan.food.presentation.util.Haptic
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { createAndroidDataStore(androidContext()) }
    single<SecureStore> { AndroidSecureStore(get()) }
    // Eager so the Vibrator is resolved at startup, not on the first impact.
    single(createdAtStart = true) { Haptic(androidContext()) }
}