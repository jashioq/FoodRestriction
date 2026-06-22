package com.jan.food.presentation.components.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures

/** Diameter of the focus reticle ring. */
private val RETICLE_DIAMETER = 64.dp

/** Stroke width of the focus reticle ring. */
private val RETICLE_STROKE = 2.dp

/** Half of the 300ms in/out fade. */
private const val FADE_HALF_MILLIS = 150

/**
 * Live camera feed with tap-to-focus. Wraps the native [CameraPreview]: tapping anywhere on the
 * feed focuses the camera on that point and shows a white ring that fades in then out over 300ms.
 *
 * @param modifier the [Modifier] applied to the feed.
 * @param onBarcodeScanned forwarded to [CameraPreview]; see its KDoc.
 */
@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String?) -> Unit = {},
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var focusRequest by remember { mutableStateOf<FocusRequest?>(null) }
    var reticleAt by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width == 0 || size.height == 0) return@detectTapGestures
                    reticleAt = offset
                    focusRequest = FocusRequest(
                        x = offset.x / size.width,
                        y = offset.y / size.height,
                        id = (focusRequest?.id ?: 0L) + 1L,
                    )
                }
            },
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onBarcodeScanned = onBarcodeScanned,
            focusRequest = focusRequest,
        )

        focusRequest?.let { request ->
            reticleAt?.let { center ->
                // Key the animation on the request id so each tap restarts the fade.
                FocusReticle(center = center, restartKey = request.id)
            }
        }
    }
}

/** White ring at [center] that fades in (150ms) then out (150ms) whenever [restartKey] changes. */
@Composable
private fun FocusReticle(center: Offset, restartKey: Long) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(restartKey) {
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(FADE_HALF_MILLIS))
        alpha.animateTo(0f, tween(FADE_HALF_MILLIS))
    }

    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        with(density) {
            drawCircle(
                color = Color.White,
                radius = RETICLE_DIAMETER.toPx() / 2f,
                center = center,
                alpha = alpha.value,
                style = Stroke(width = RETICLE_STROKE.toPx()),
            )
        }
    }
}
