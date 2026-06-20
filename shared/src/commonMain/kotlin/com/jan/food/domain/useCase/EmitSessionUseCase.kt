package com.jan.food.domain.useCase

import com.jan.food.domain.model.AuthSession
import com.jan.food.domain.repository.AuthRepository
import com.jan.food.domain.util.UseCase
import kotlinx.coroutines.flow.Flow

/**
 * Use case used for emitting the current authentication session as a [Flow],
 * re-emitting on every login/logout (`null` = logged out).
 * @param authRepository an [AuthRepository] instance.
 */
open class EmitSessionUseCase(
    private val authRepository: AuthRepository,
) : UseCase<Unit, Flow<AuthSession?>> {
    /**
     * @return a [Flow] of the current [AuthSession], or `null` when logged out.
     */
    override suspend fun call(value: Unit): Result<Flow<AuthSession?>> =
        authRepository.emitSession()
}
