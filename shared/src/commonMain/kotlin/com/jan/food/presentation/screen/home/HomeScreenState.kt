package com.jan.food.presentation.screen.home

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.model.AuthSession
import com.jan.food.domain.model.ProductCheck
import com.jan.food.presentation.components.camera.CameraFeedAnchor

data class HomeScreenState(
    val session: AuthSession?,
    val productCheck: ProductCheck?,
    val isLoading: Boolean,
    val selectedAllergens: List<Allergen>,
)

/**
 * Maps the screen state to the camera feed slide position: loading reveals a quarter strip, results
 * reveal three quarters, and the idle state keeps the feed full-screen.
 */
fun HomeScreenState.cameraFeedAnchor(): CameraFeedAnchor = when {
    isLoading -> CameraFeedAnchor.REVEAL_QUARTER
    productCheck != null -> CameraFeedAnchor.REVEAL_THREE_QUARTERS
    else -> CameraFeedAnchor.FULL
}
