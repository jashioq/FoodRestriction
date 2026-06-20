package com.jan.food.di

import com.jan.food.data.dataSource.AndroidSecureStore
import com.jan.food.data.dataSource.SecureStore
import com.jan.food.data.dataSource.createAndroidDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformDataSourceModule = module {
    single { createAndroidDataStore(androidContext()) }
    single<SecureStore> { AndroidSecureStore(get()) }
}