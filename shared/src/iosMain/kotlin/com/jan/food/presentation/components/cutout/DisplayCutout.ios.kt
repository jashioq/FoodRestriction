package com.jan.food.presentation.components.cutout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen
import platform.posix.getenv
import platform.posix.uname
import platform.posix.utsname

/**
 * Resolves the iOS cutout by device model identifier (e.g. `iPhone15,2`) and derives its bounding
 * box from known per-family dimensions, centered at the top of the screen. The app's minimum is
 * iOS 18.2, so the oldest supported devices are the iPhone XR/XS generation and iPhone SE (2nd gen).
 * iOS points map 1:1 to [androidx.compose.ui.unit.Dp].
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDisplayCutout(): DisplayCutout = remember {
    val type = cutoutTypeForIdentifier(deviceModelIdentifier())
    val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }

    when (type) {
        DisplayCutoutType.DYNAMIC_ISLAND -> centered(type, screenWidth, width = 126.0, height = 37.33, top = 11.0)
        DisplayCutoutType.LARGE_NOTCH -> centered(type, screenWidth, width = 209.0, height = 30.0, top = 0.0)
        DisplayCutoutType.SMALL_NOTCH -> centered(type, screenWidth, width = 162.0, height = 33.0, top = 0.0)
        DisplayCutoutType.NO_CUTOUT, DisplayCutoutType.CUSTOM ->
            DisplayCutout(type, DpOffset.Zero, DpSize.Zero)
    }
}

/** iOS doesn't expose a cutout outline path; callers fall back to the per-type derived shape. */
@Composable
actual fun rememberCutoutOutlinePath(): Path? = null

/** Builds a [DisplayCutout] whose bounding box is horizontally centered at the top of the screen. */
private fun centered(
    type: DisplayCutoutType,
    screenWidth: Double,
    width: Double,
    height: Double,
    top: Double,
): DisplayCutout = DisplayCutout(
    type = type,
    offset = DpOffset(((screenWidth - width) / 2).dp, top.dp),
    size = DpSize(width.dp, height.dp),
)

private fun cutoutTypeForIdentifier(identifier: String): DisplayCutoutType =
    DeviceCutoutMap[identifier] ?: DisplayCutoutType.DYNAMIC_ISLAND

/**
 * Reads the hardware model identifier via `uname`. On the simulator `uname` reports the host arch
 * (`arm64`/`x86_64`), so we fall back to the `SIMULATOR_MODEL_IDENTIFIER` env var the simulator sets.
 */
@OptIn(ExperimentalForeignApi::class)
private fun deviceModelIdentifier(): String {
    val machine = memScoped {
        val info = alloc<utsname>()
        uname(info.ptr)
        info.machine.toKString()
    }
    if (machine == "arm64" || machine == "x86_64" || machine == "i386") {
        val simulated = getenv("SIMULATOR_MODEL_IDENTIFIER")?.toKString()
        if (!simulated.isNullOrEmpty()) return simulated
    }
    return machine
}

/**
 * Maps iOS device model identifiers to their cutout silhouette. Covers every iPhone that can run the
 * app's minimum iOS 18.2. Identifiers not listed fall through to the [DisplayCutoutType.DYNAMIC_ISLAND]
 * default in [cutoutTypeForIdentifier].
 */
private val DeviceCutoutMap: Map<String, DisplayCutoutType> = buildMap {
    // No cutout — Touch ID, home button.
    put("iPhone12,8", DisplayCutoutType.NO_CUTOUT) // iPhone SE (2nd gen)
    put("iPhone14,6", DisplayCutoutType.NO_CUTOUT) // iPhone SE (3rd gen)

    // Large notch — iPhone XR/XS through iPhone 12.
    put("iPhone11,8", DisplayCutoutType.LARGE_NOTCH) // iPhone XR
    put("iPhone11,2", DisplayCutoutType.LARGE_NOTCH) // iPhone XS
    put("iPhone11,4", DisplayCutoutType.LARGE_NOTCH) // iPhone XS Max
    put("iPhone11,6", DisplayCutoutType.LARGE_NOTCH) // iPhone XS Max
    put("iPhone12,1", DisplayCutoutType.LARGE_NOTCH) // iPhone 11
    put("iPhone12,3", DisplayCutoutType.LARGE_NOTCH) // iPhone 11 Pro
    put("iPhone12,5", DisplayCutoutType.LARGE_NOTCH) // iPhone 11 Pro Max
    put("iPhone13,1", DisplayCutoutType.LARGE_NOTCH) // iPhone 12 mini
    put("iPhone13,2", DisplayCutoutType.LARGE_NOTCH) // iPhone 12
    put("iPhone13,3", DisplayCutoutType.LARGE_NOTCH) // iPhone 12 Pro
    put("iPhone13,4", DisplayCutoutType.LARGE_NOTCH) // iPhone 12 Pro Max

    // Small notch — iPhone 13/14 (non-Pro) and iPhone 16e.
    put("iPhone14,4", DisplayCutoutType.SMALL_NOTCH) // iPhone 13 mini
    put("iPhone14,5", DisplayCutoutType.SMALL_NOTCH) // iPhone 13
    put("iPhone14,2", DisplayCutoutType.SMALL_NOTCH) // iPhone 13 Pro
    put("iPhone14,3", DisplayCutoutType.SMALL_NOTCH) // iPhone 13 Pro Max
    put("iPhone14,7", DisplayCutoutType.SMALL_NOTCH) // iPhone 14
    put("iPhone14,8", DisplayCutoutType.SMALL_NOTCH) // iPhone 14 Plus
    put("iPhone17,5", DisplayCutoutType.SMALL_NOTCH) // iPhone 16e

    // Dynamic Island — iPhone 14 Pro and later.
    put("iPhone15,2", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 14 Pro
    put("iPhone15,3", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 14 Pro Max
    put("iPhone15,4", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 15
    put("iPhone15,5", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 15 Plus
    put("iPhone16,1", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 15 Pro
    put("iPhone16,2", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 15 Pro Max
    put("iPhone17,3", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 16
    put("iPhone17,4", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 16 Plus
    put("iPhone17,1", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 16 Pro
    put("iPhone17,2", DisplayCutoutType.DYNAMIC_ISLAND) // iPhone 16 Pro Max
}
