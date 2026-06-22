package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Full-bleed live camera feed from the device's back camera, configured for a 60 fps preview
 * (falling back to the platform default where unsupported). Renders a native preview surface
 * (CameraX on Android, AVFoundation on iOS), requests camera permission on first display, and
 * continuously scans for barcodes.
 *
 * @param modifier the [Modifier] applied to the preview surface.
 * @param onBarcodeScanned invoked with a decoded barcode as soon as one is detected (and again
 * whenever the visible barcode changes), and with `null` once no barcode has been visible for a
 * few seconds.
 * @param focusRequest the latest point to focus the camera on; each new (distinct) value focuses
 * the camera on that normalized point. `null` leaves the camera in its default focus mode.
 */
@Composable
expect fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String?) -> Unit = {},
    focusRequest: FocusRequest? = null,
)
