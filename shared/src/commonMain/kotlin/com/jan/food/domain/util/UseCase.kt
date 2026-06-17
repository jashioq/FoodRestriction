package com.jan.food.domain.util

/**
 * A generic interface that governs basic interactions for every use case.
 * @param Val the type of the input parameter.
 * @param Res the type of the output parameter.
 */
interface UseCase<in Val, out Res> {
    suspend fun call(value: Val): Result<Res>
}
