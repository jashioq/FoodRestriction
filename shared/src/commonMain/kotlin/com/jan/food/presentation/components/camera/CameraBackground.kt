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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fallback top-corner radius when the device's screen curvature can't be read (see [rememberDeviceCornerRadius]). */
private val DefaultFeedCornerRadius = 45.dp

/** Blur radius of the custom drop shadow drawn behind the feed. */
private val FeedShadowBlur = 28.dp

/** Color (incl. alpha) of the custom drop shadow drawn behind the feed. */
private val FeedShadowColor = Color.Black.copy(alpha = 0.6f)

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

    // Match the slid-down feed's corners to the device screen curvature, falling back to a default
    // where the platform can't report it.
    val deviceCorner = rememberDeviceCornerRadius()
    val feedCornerRadius = if (deviceCorner != Dp.Unspecified) deviceCorner else DefaultFeedCornerRadius

    // Same target + same spec as rememberCameraFeedOffsetFraction(), so the feed and any
    // feed-attached screen content animate frame-for-frame in sync.
    val offsetFraction by animateFloatAsState(
        targetValue = controller.anchor.offsetFraction,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (controller.anchor == CameraFeedAnchor.FULL) 0.dp else feedCornerRadius,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )
    val darkenAlpha by animateFloatAsState(
        targetValue = if (controller.darken) DarkenAlpha else 0f,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // The feed's slide geometry + rounded-corner shape, shared by the preview and darken scrim.
        val feedShape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        val feedGeometry = Modifier
            .fillMaxSize()
            .offset(y = maxHeight * offsetFraction)

        // White backdrop, only seen once the feed slides down and reveals it.
        Box(modifier = Modifier.fillMaxSize().background(Color.White))

        // Custom drop shadow: a dark rounded shape matching the feed, blurred so it bleeds out and
        // reads strongly against the white backdrop once slid. Off-screen and unseen at the full
        // anchor. Drawn behind the feed.
        Box(
            modifier = feedGeometry
                .blur(radius = FeedShadowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(color = FeedShadowColor, shape = feedShape),
        )

        CameraPreview(
            modifier = feedGeometry.clip(feedShape),
            onBarcodeScanned = { barcode -> latestBarcode = barcode },
            blurred = controller.blurred,
            tapToFocusEnabled = controller.tapToFocus && controller.anchor == CameraFeedAnchor.FULL,
        )

        if (darkenAlpha > 0f) {
            Box(modifier = feedGeometry.clip(feedShape).background(Color.Black.copy(alpha = darkenAlpha)))
        }

        CompositionLocalProvider(LocalCameraFeedController provides controller) {
            content(latestBarcode)
        }
    }
}
