package com.jan.food.di

import com.jan.food.data.dataSource.createAndroidDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val dataSourceModule = module {
    single { createAndroidDataStore(androidContext()) }
}