package com.jan.food.domain.repository

import com.jan.food.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /**
     * Authenticate a user with their email and password and persist the resulting session.
     * @param email the user's email.
     * @param password the user's password.
     */
    suspend fun login(
        email: String,
        password: String,
    ): Result<Unit>

    /**
     * Clear the persisted session, logging the user out.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Emit the persisted session as a [Flow], re-emitting on every login/logout
     * (`null` = logged out). Subscribe to react to auth state instead of polling.
     */
    suspend fun emitSession(): Result<Flow<AuthSession?>>

    /**
     * Refresh the persisted session using its refresh token and persist the result.
     */
    suspend fun refreshSession(): Result<AuthSession>
}
