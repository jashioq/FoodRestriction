package com.jan.food.presentation.components.camera

/**
 * A request to focus the camera on a single point of the preview.
 *
 * @param x normalized horizontal position in the preview, `0f` (left) .. `1f` (right).
 * @param y normalized vertical position in the preview, `0f` (top) .. `1f` (bottom).
 * @param id monotonic identifier, incremented per tap, so two requests at the same point are still
 * distinct values and re-trigger the platform focus effect.
 */
data class FocusRequest(
    val x: Float,
    val y: Float,
    val id: Long,
)
