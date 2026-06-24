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

/** Alpha of the black scrim when the feed is darkened. */
private const val DarkenAlpha = 0.8f

/**
 * Hosts a single, persistent [CameraPreview] as a full-screen background and draws [content] on
 * top of it. Because the feed lives here rather than inside each screen, it starts once and keeps
 * running across navigation — callers never re-create it, avoiding a white flash on screen entry.
 *
 * Every adjustable property of the feed (slide position, blur, tap-to-focus, darken) is centralized
 * in a [CameraFeedController], provided to [content] via [LocalCameraFeedController]. Screens drive
 * it declaratively with [CameraFeedEffect] and align their own content with the slide via
 * [rememberCameraFeedOffsetFraction]. The most recently scanned barcode is passed to [content].
 *
 * @param modifier the [Modifier] applied to the background container.
 * @param content the foreground drawn over the feed, given the latest scanned barcode (or `null`
 * once none has been visible for a few seconds).
 */
@Composable
fun CameraBackground(
    modifier: Modifier = Modifier,
    content: @Composable (barcode: String?) -> Unit,
) {
    var latestBarcode by remember { mutableStateOf<String?>(null) }
    val controller = remember { CameraFeedController() }

    // Same target + same spec as rememberCameraFeedOffsetFraction(), so the feed and any
    // feed-attached screen content animate frame-for-frame in sync.
    val offsetFraction by animateFloatAsState(
        targetValue = controller.anchor.offsetFraction,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (controller.anchor == CameraFeedAnchor.FULL) 0.dp else FeedCornerRadius,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )
    val darkenAlpha by animateFloatAsState(
        targetValue = if (controller.darken) DarkenAlpha else 0f,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // The feed's slide + rounded-corner geometry, shared by the preview and the darken scrim.
        val feedModifier = Modifier
            .fillMaxSize()
            .offset(y = maxHeight * offsetFraction)
            .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))

        // White backdrop, only seen once the feed slides down and reveals it.
        Box(modifier = Modifier.fillMaxSize().background(Color.White))

        CameraPreview(
            modifier = feedModifier,
            onBarcodeScanned = { barcode -> latestBarcode = barcode },
            blurred = controller.blurred,
            tapToFocusEnabled = controller.tapToFocus && controller.anchor == CameraFeedAnchor.FULL,
        )

        if (darkenAlpha > 0f) {
            Box(modifier = feedModifier.background(Color.Black.copy(alpha = darkenAlpha)))
        }

        CompositionLocalProvider(LocalCameraFeedController provides controller) {
            content(latestBarcode)
        }
    }
}
