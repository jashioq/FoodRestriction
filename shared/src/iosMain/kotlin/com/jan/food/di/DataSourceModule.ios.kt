package com.jan.food.di

import com.jan.food.data.dataSource.IosSecureStore
import com.jan.food.data.dataSource.SecureStore
import com.jan.food.data.dataSource.createIosDataStore
import org.koin.dsl.module

actual val platformDataSourceModule = module {
    single { createIosDataStore() }
    single<SecureStore> { IosSecureStore() }
}