package com.jan.food.presentation.components.camera

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize

/**
 * Reads the Android display cutout (API 28+) from the window insets. The platform reports one or
 * more bounding rects in window pixel coordinates; we take their union as the cutout bounding box
 * and classify any cutout as [DisplayCutoutType.CUSTOM]. Its absence is [DisplayCutoutType.NO_CUTOUT].
 */
@Composable
actual fun rememberDisplayCutout(): DisplayCutout {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return DisplayCutout(DisplayCutoutType.NO_CUTOUT, DpOffset.Zero, DpSize.Zero)
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val rects = view.rootWindowInsets?.displayCutout?.boundingRects
    if (rects.isNullOrEmpty()) {
        return DisplayCutout(DisplayCutoutType.NO_CUTOUT, DpOffset.Zero, DpSize.Zero)
    }

    val left = rects.minOf { it.left }
    val top = rects.minOf { it.top }
    val right = rects.maxOf { it.right }
    val bottom = rects.maxOf { it.bottom }

    return with(density) {
        DisplayCutout(
            type = DisplayCutoutType.CUSTOM,
            offset = DpOffset(left.toDp(), top.toDp()),
            size = DpSize((right - left).toDp(), (bottom - top).toDp()),
        )
    }
}

/**
 * Returns the exact cutout outline as a [Path] in window pixel coordinates (API 31+). The app draws
 * edge-to-edge, so these coordinates align 1:1 with a full-screen Compose canvas at the origin.
 */
@Composable
actual fun rememberCutoutOutlinePath(): Path? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

    val view = LocalView.current
    val path = view.rootWindowInsets?.displayCutout?.cutoutPath ?: return null
    return path.asComposePath()
}
