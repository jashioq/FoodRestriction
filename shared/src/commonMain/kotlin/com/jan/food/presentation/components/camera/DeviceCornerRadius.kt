package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * The device's physical screen corner radius, used to round the slid-down camera feed so it matches
 * the screen's curvature on any device. Returns [Dp.Unspecified] when the platform can't report it
 * (e.g. older Android, square-cornered or external displays, desktop), letting callers fall back to
 * a sensible default.
 */
@Composable
expect fun rememberDeviceCornerRadius(): Dp
