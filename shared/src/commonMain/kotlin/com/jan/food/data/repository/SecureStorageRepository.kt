package com.jan.food.data.repository

import com.jan.food.data.dataSource.SecureStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SecureStorageRepository(
    private val secureStore: SecureStore,
) : com.jan.food.domain.repository.SecureStorageRepository {

    override suspend fun putSecureString(key: String, value: String): Result<Unit> =
        runCatching { secureStore.put(key, value) }

    override suspend fun emitSecureString(key: String, default: String): Result<Flow<String>> =
        runCatching {
            secureStore.observe(key)
                .map { it ?: default }
                .distinctUntilChanged()
        }

    override suspend fun clearSecureString(key: String): Result<Unit> =
        runCatching { secureStore.remove(key) }
}
