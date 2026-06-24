package com.jan.food.presentation.components.camera

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp

/**
 * Reads the top-left rounded corner radius reported by the window insets (Android 12 / API 31+).
 * Returns [Dp.Unspecified] on older versions or when the display has no rounded corners.
 */
@Composable
actual fun rememberDeviceCornerRadius(): Dp {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return Dp.Unspecified

    val view = LocalView.current
    val density = LocalDensity.current
    val corner = view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
        ?: return Dp.Unspecified

    return with(density) { corner.radius.toDp() }
}
