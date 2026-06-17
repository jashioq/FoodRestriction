package com.jan.food.di

import com.jan.food.data.dataSource.createIosDataStore
import org.koin.dsl.module

actual val dataSourceModule = module {
    single { createIosDataStore() }
}