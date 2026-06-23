package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Full-bleed live camera feed from the device's back camera, configured for a 60 fps preview
 * (falling back to the platform default where unsupported). Renders a native preview surface
 * (CameraX on Android, AVFoundation on iOS), requests camera permission on first display, and
 * continuously scans for barcodes.
 *
 * Android adds tap-to-focus (with a focus reticle) when [tapToFocusEnabled]; iOS relies on its
 * automatic macro lens and continuous autofocus, so it has no tap interaction regardless.
 *
 * Blur is handled natively per platform (Compose's `Modifier.blur` can't blur the iOS native
 * preview surface), so callers toggle it through [blurred] rather than applying a blur modifier.
 *
 * @param modifier the [Modifier] applied to the preview surface.
 * @param onBarcodeScanned invoked with a decoded barcode as soon as one is detected (and again
 * whenever the visible barcode changes), and with `null` once no barcode has been visible for a
 * few seconds.
 * @param blurred when true, the feed animates to a blurred state (for use as a backdrop); when
 * false, back to a sharp live preview.
 * @param tapToFocusEnabled when true (Android only), tapping the feed focuses/meters on that point
 * and draws a focus reticle; when false, taps are ignored and no reticle is drawn.
 */
@Composable
expect fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String?) -> Unit = {},
    blurred: Boolean = false,
    tapToFocusEnabled: Boolean = true,
)
