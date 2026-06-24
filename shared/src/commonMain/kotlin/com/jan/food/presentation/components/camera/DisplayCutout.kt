package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable

/**
 * The kind of top-edge display cutout the device has, used to shape effects (e.g. the spinning
 * alarm beam) that wrap around it. Coarser than exact geometry on purpose: the beam only needs to
 * know the cutout's silhouette family, not its pixels.
 */
enum class DisplayCutoutType {
    /** No cutout — older devices with bezels/home button (e.g. iPhone SE, iPhone 8). */
    NO_CUTOUT,

    /** Pill-shaped Dynamic Island (iPhone 14 Pro and later Pro/standard models). */
    DYNAMIC_ISLAND,

    /** Wide original-style notch (iPhone X through 12). */
    LARGE_NOTCH,

    /** Narrower notch (iPhone 13/14 and iPhone 16e). */
    SMALL_NOTCH,

    /** Android punch-hole / custom cutout reported by the platform window insets. */
    CUSTOM,
}

/**
 * Detects the device's [DisplayCutoutType]. On Android this is derived live from the window insets'
 * display cutout; on iOS it is looked up from the device model identifier. Returns
 * [DisplayCutoutType.NO_CUTOUT] when the platform reports no cutout or can't be classified.
 */
@Composable
expect fun rememberDisplayCutoutType(): DisplayCutoutType
