package com.jan.food.data.dataSource

import kotlinx.coroutines.flow.Flow

interface SecureStore {
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
    fun observe(key: String): Flow<String?>
}
