package com.jan.food.presentation.components.camera

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/**
 * Classifies the Android display cutout. The platform exposes a [android.view.DisplayCutout] (API
 * 28+) with bounding rects for any punch-hole/notch; we don't try to distinguish their shapes, so
 * any reported cutout is [DisplayCutoutType.CUSTOM] and its absence is [DisplayCutoutType.NO_CUTOUT].
 */
@Composable
actual fun rememberDisplayCutoutType(): DisplayCutoutType {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return DisplayCutoutType.NO_CUTOUT

    val view = LocalView.current
    val cutout = view.rootWindowInsets?.displayCutout
    val hasCutout = cutout?.boundingRects?.isNotEmpty() == true
    return if (hasCutout) DisplayCutoutType.CUSTOM else DisplayCutoutType.NO_CUTOUT
}
