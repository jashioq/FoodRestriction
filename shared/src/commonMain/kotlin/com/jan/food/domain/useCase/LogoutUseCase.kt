package com.jan.food.domain.useCase

import com.jan.food.domain.repository.AuthRepository
import com.jan.food.domain.util.UseCase

/**
 * Use case used for logging the user out by clearing the persisted session.
 * @param authRepository an [AuthRepository] instance.
 */
open class LogoutUseCase(
    private val authRepository: AuthRepository,
) : UseCase<Unit, Unit> {
    override suspend fun call(value: Unit): Result<Unit> =
        authRepository.logout()
}
