package com.jan.food.domain.useCase

import com.jan.food.domain.repository.AuthRepository
import com.jan.food.domain.util.UseCase

/**
 * Parameters for [LoginUseCase].
 * @param email the user's email.
 * @param password the user's password.
 */
data class LoginParams(
    val email: String,
    val password: String,
)

/**
 * Use case used for authenticating a user with their email and password.
 * @param authRepository an [AuthRepository] instance.
 */
open class LoginUseCase(
    private val authRepository: AuthRepository,
) : UseCase<LoginParams, Unit> {
    /**
     * @param value the [LoginParams] holding the user's credentials.
     */
    override suspend fun call(value: LoginParams): Result<Unit> =
        authRepository.login(
            email = value.email,
            password = value.password,
        )
}
