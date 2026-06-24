package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import platform.Foundation.NSNumber
import platform.Foundation.valueForKey
import platform.UIKit.UIScreen

/**
 * Reads the screen corner radius from `UIScreen`'s private `_displayCornerRadius` key (points map
 * 1:1 to [Dp]). Returns [Dp.Unspecified] if the value is unavailable.
 *
 * Note: `_displayCornerRadius` is a private API — App Store review may flag it; swap for a constant
 * if that becomes a concern.
 */
@Composable
actual fun rememberDeviceCornerRadius(): Dp {
    val radius = (UIScreen.mainScreen.valueForKey("_displayCornerRadius") as? NSNumber)?.doubleValue
    return radius?.dp ?: Dp.Unspecified
}
