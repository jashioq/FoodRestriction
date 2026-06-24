package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.uname
import platform.posix.utsname

/**
 * Classifies the iOS cutout by device model identifier (e.g. `iPhone15,2`). The app's minimum is
 * iOS 18.2, so the oldest supported devices are the iPhone XR/XS generation and iPhone SE (2nd gen);
 * everything earlier is irrelevant. Unknown/newer identifiers default to [DisplayCutoutType.DYNAMIC_ISLAND],
 * the current iPhone silhouette.
 */
@Composable
actual fun rememberDisplayCutoutType(): DisplayCutoutType = remember {
    cutoutTypeForIdentifier(deviceModelIdentifier())
}

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
