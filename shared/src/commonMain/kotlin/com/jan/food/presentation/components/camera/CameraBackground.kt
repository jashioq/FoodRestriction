package com.jan.food.presentation.components.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Hosts a single, persistent [CameraPreview] as a full-screen background and draws [content] on
 * top of it. Because the feed lives here rather than inside each screen, it starts once and keeps
 * running across navigation — callers never re-create it, avoiding a white flash on screen entry.
 *
 * The feed blurs (natively, per platform) whenever [blurred] is true, e.g. when an overlay screen
 * is shown over it. The most recently scanned barcode is passed to [content].
 *
 * @param blurred when true, the feed animates to a blurred state; when false, back to sharp.
 * @param modifier the [Modifier] applied to the background container.
 * @param tapToFocusEnabled when true, tapping the feed triggers tap-to-focus (Android only); when
 * false, taps are ignored and no focus reticle is drawn.
 * @param content the foreground drawn over the feed, given the latest scanned barcode (or `null`
 * once none has been visible for a few seconds).
 */
@Composable
fun CameraBackground(
    blurred: Boolean,
    modifier: Modifier = Modifier,
    tapToFocusEnabled: Boolean = true,
    content: @Composable (barcode: String?) -> Unit,
) {
    var latestBarcode by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onBarcodeScanned = { barcode -> latestBarcode = barcode },
            blurred = blurred,
            tapToFocusEnabled = tapToFocusEnabled,
        )

        content(latestBarcode)
    }
}
