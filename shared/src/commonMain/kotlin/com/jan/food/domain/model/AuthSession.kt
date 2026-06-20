package com.jan.food.domain.model

import kotlinx.serialization.Serializable

/**
 * An authenticated session, persisted in secure storage.
 * @param idToken the identity token, sent as the bearer to the app API.
 * @param accessToken the access token.
 * @param refreshToken the refresh token, used to obtain fresh tokens when they expire.
 */
@Serializable
data class AuthSession(
    val idToken: String,
    val accessToken: String,
    val refreshToken: String,
)
