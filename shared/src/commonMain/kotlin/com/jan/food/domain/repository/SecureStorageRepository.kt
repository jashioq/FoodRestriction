package com.jan.food.domain.repository

import kotlinx.coroutines.flow.Flow

interface SecureStorageRepository {
    /**
     * Write [String] value in secure storage.
     * @param key a key that will identify the value.
     * @param value a value to be written.
     */
    suspend fun putSecureString(
        key: String,
        value: String,
    ): Result<Unit>

    /**
     * Emit [String] value from secure storage as a [Flow].
     * @param key a key of the value to be emitted.
     * @param default a default value to be emitted if the key is not present.
     */
    suspend fun emitSecureString(
        key: String,
        default: String,
    ): Result<Flow<String>>

    /**
     * Remove [String] value from secure storage.
     * @param key a key of the value to be removed.
     */
    suspend fun clearSecureString(
        key: String,
    ): Result<Unit>
}