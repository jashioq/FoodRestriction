package com.jan.food.di

import com.jan.food.data.repository.DataStoreRepository
import com.jan.food.data.repository.SecureStorageRepository
import org.koin.dsl.module

val dataModule = module {
    single<com.jan.food.domain.repository.DataStoreRepository> {
        DataStoreRepository(dataStore = get())
    }
    single<com.jan.food.domain.repository.SecureStorageRepository> {
        SecureStorageRepository(secureStore = get())
    }
}