package com.jan.food.data.dataSource.auth

/**
 * Contract for the AWS Cognito `InitiateAuth` endpoint.
 * Production implementation: [HttpCognitoAuthClient].
 */
interface CognitoAuthClient {
    /**
     * Authenticate a user with their username and password via the `USER_PASSWORD_AUTH` flow.
     * @param username the user's username (their email).
     * @param password the user's password.
     */
    suspend fun loginWithPassword(
        username: String,
        password: String,
    ): Result<AuthenticationResult>

    /**
     * Exchange a refresh token for fresh tokens via the `REFRESH_TOKEN_AUTH` flow.
     * @param refreshToken the refresh token issued by a previous authentication.
     */
    suspend fun refresh(refreshToken: String): Result<AuthenticationResult>
}
