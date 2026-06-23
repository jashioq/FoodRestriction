package com.jan.food.presentation.components.camera

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hosts a single, persistent [CameraPreview] as a full-screen background and draws [content] on
 * top of it. Because the feed lives here rather than inside each screen, it starts once and keeps
 * running across navigation — callers never re-create it, avoiding a white flash on screen entry.
 *
 * The feed blurs (animating to [blurRadius]) whenever [blurred] is true, e.g. when an overlay
 * screen is shown over it. The most recently scanned barcode is passed to [content].
 *
 * @param blurred when true, the feed animates to a blurred state; when false, back to sharp.
 * @param modifier the [Modifier] applied to the background container.
 * @param blurRadius the blur radius applied while [blurred] is true.
 * @param content the foreground drawn over the feed, given the latest scanned barcode (or `null`
 * once none has been visible for a few seconds).
 */
@Composable
fun CameraBackground(
    blurred: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 40.dp,
    content: @Composable (barcode: String?) -> Unit,
) {
    val radius by animateDpAsState(
        targetValue = if (blurred) blurRadius else 0.dp,
        label = "cameraBlur",
    )

    var latestBarcode by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius, BlurredEdgeTreatment.Rectangle),
            onBarcodeScanned = { barcode -> latestBarcode = barcode },
        )

        content(latestBarcode)
    }
}
