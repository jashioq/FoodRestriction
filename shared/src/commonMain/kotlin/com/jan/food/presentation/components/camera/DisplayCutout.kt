package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize

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
 * The device's top-edge cutout: its [type] plus the bounding box of the cutout, expressed relative
 * to the top-left of the full-screen window so a full-bleed overlay can position content over it.
 * For [DisplayCutoutType.NO_CUTOUT] the [offset]/[size] are [DpOffset.Zero]/[DpSize.Zero].
 *
 * @param type the cutout silhouette family.
 * @param offset the top-left of the cutout bounding box from the screen's top-left corner.
 * @param size the size of the cutout bounding box.
 */
data class DisplayCutout(
    val type: DisplayCutoutType,
    val offset: DpOffset,
    val size: DpSize,
)

/**
 * Detects the device's [DisplayCutout]. On Android the geometry is read live from the window insets'
 * display cutout; on iOS the [type] is looked up from the device model identifier and the geometry
 * is derived from known per-family dimensions centered at the top of the screen.
 */
@Composable
expect fun rememberDisplayCutout(): DisplayCutout

/**
 * The exact outline of the display cutout as a [Path] in pixel coordinates (the screen's top-left is
 * the origin), when the platform can report it — Android API 31+. Returns `null` when unavailable
 * (iOS, older Android, or no cutout), in which case callers fall back to the [DisplayCutout] bounding
 * box and per-type shape. The path traces the true silhouette, so it matches a circular punch-hole
 * exactly rather than approximating it with a bounding box.
 */
@Composable
expect fun rememberCutoutOutlinePath(): Path?
