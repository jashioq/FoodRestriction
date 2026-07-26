package com.jan.food.fixtures

import com.jan.food.data.dataSource.auth.AuthenticationResult
import com.jan.food.domain.model.AuthSession

/**
 * Builds a sample [AuthSession] domain model. Override only the fields a test asserts on.
 */
fun authSession(
    idToken: String = "id-token",
    accessToken: String = "access-token",
    refreshToken: String = "refresh-token",
): AuthSession =
    AuthSession(
        idToken = idToken,
        accessToken = accessToken,
        refreshToken = refreshToken,
    )

/**
 * Builds a sample Cognito [AuthenticationResult]. [refreshToken] is nullable because
 * `REFRESH_TOKEN_AUTH` responses omit it.
 */
fun authenticationResult(
    idToken: String = "id-token",
    accessToken: String = "access-token",
    refreshToken: String? = "refresh-token",
    expiresIn: Int = 3600,
    tokenType: String = "Bearer",
): AuthenticationResult =
    AuthenticationResult(
        idToken = idToken,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        tokenType = tokenType,
    )
