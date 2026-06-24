package com.jan.food.presentation.components.camera

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Top-corner radius the feed rounds to when slid down, approximating the device screen curvature. */
private val FeedCornerRadius = 44.dp

/**
 * Hosts a single, persistent [CameraPreview] as a full-screen background and draws [content] on
 * top of it. Because the feed lives here rather than inside each screen, it starts once and keeps
 * running across navigation — callers never re-create it, avoiding a white flash on screen entry.
 *
 * The feed blurs (natively, per platform) whenever [blurred] is true, e.g. when an overlay screen
 * is shown over it. The most recently scanned barcode is passed to [content].
 *
 * The feed can also slide down (revealing a white backdrop above it, with rounded top corners) under
 * the control of a [CameraFeedController], provided to [content] via [LocalCameraFeedController].
 * Screens drive it declaratively with [CameraFeedAnchorEffect] and align their own content with it
 * via [rememberCameraFeedOffsetFraction].
 *
 * @param blurred when true, the feed animates to a blurred state; when false, back to sharp.
 * @param modifier the [Modifier] applied to the background container.
 * @param tapToFocusEnabled when true, tapping the feed triggers tap-to-focus (Android only); when
 * false, taps are ignored and no focus reticle is drawn. Focus is additionally suppressed whenever
 * the feed is slid away from [CameraFeedAnchor.FULL].
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
    val controller = remember { CameraFeedController() }

    // Same target + same spec as rememberCameraFeedOffsetFraction(), so the feed and any
    // feed-attached screen content animate frame-for-frame in sync.
    val offsetFraction by animateFloatAsState(
        targetValue = controller.targetAnchor.offsetFraction,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (controller.targetAnchor == CameraFeedAnchor.FULL) 0.dp else FeedCornerRadius,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // White backdrop, only seen once the feed slides down and reveals it.
        Box(modifier = Modifier.fillMaxSize().background(Color.White))

        CameraPreview(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = maxHeight * offsetFraction)
                .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)),
            onBarcodeScanned = { barcode -> latestBarcode = barcode },
            blurred = blurred,
            tapToFocusEnabled = tapToFocusEnabled && controller.targetAnchor == CameraFeedAnchor.FULL,
        )

        CompositionLocalProvider(LocalCameraFeedController provides controller) {
            content(latestBarcode)
        }
    }
}
