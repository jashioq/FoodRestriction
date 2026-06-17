package com.jan.food.domain.repository

import kotlinx.coroutines.flow.Flow

interface DataStoreRepository {
    /**
     * Write [Int] value in the local datastore.
     * @param key a key that will identify the value.
     * @param value a value to be written.
     */
    suspend fun putIntPreference(
        key: String,
        value: Int,
    ): Result<Unit>

    /**
     * Emit [Int] value from the local datastore as a [Flow].
     * @param key a key of the value to be emitted.
     * @param default a default value to be emitted if the key is not in the datastore.
     */
    suspend fun emitIntPreference(
        key: String,
        default: Int,
    ): Result<Flow<Int>>

    /**
     * Write [String] value in the local datastore.
     * @param key a key that will identify the value.
     * @param value a value to be written.
     */
    suspend fun putStringPreference(
        key: String,
        value: String,
    ): Result<Unit>

    /**
     * Emit [String] value from the local datastore as a [Flow].
     * @param key a key of the value to be emitted.
     * @param default a default value to be emitted if the key is not in the datastore.
     */
    suspend fun emitStringPreference(
        key: String,
        default: String,
    ): Result<Flow<String>>

    /**
     * Write [Boolean] value in the local datastore.
     * @param key a key that will identify the value.
     * @param value a value to be written.
     */
    suspend fun putBooleanPreference(
        key: String,
        value: Boolean,
    ): Result<Unit>

    /**
     * Emit [Boolean] value from the local datastore as a [Flow].
     * @param key a key of the value to be emitted.
     * @param default a default value to be emitted if the key is not in the datastore.
     */
    suspend fun emitBooleanPreference(
        key: String,
        default: Boolean,
    ): Result<Flow<Boolean>>

    /**
     * Write [Float] value in the local datastore.
     * @param key a key that will identify the value.
     * @param value a value to be written.
     */
    suspend fun putFloatPreference(
        key: String,
        value: Float,
    ): Result<Unit>

    /**
     * Emit [Float] value from the local datastore as a [Flow].
     * @param key a key of the value to be emitted.
     * @param default a default value to be emitted if the key is not in the datastore.
     */
    suspend fun emitFloatPreference(
        key: String,
        default: Float,
    ): Result<Flow<Float>>
}
