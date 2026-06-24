package com.jan.food.presentation.components.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Duration of the shared camera feed's slide/corner animation. */
internal const val CameraFeedSlideMillis = 500

/** Easing of the shared camera feed's slide/corner animation. */
internal val CameraFeedSlideEasing = FastOutSlowInEasing

/**
 * Slide positions ("anchors") for the shared camera feed, expressed as the fraction of the screen
 * height the feed is translated **down** by. [FULL] is the default, full-screen position.
 *
 * @param offsetFraction how far down the feed slides, as a fraction of the screen height.
 */
enum class CameraFeedAnchor(val offsetFraction: Float) {
    FULL(0f),
    REVEAL_QUARTER(0.25f),
    REVEAL_THREE_QUARTERS(0.75f),
}

/**
 * Controls the slide position of the single shared camera feed hosted by [CameraBackground]. Obtain
 * it from anywhere under the feed via [LocalCameraFeedController]; drive it declaratively with
 * [CameraFeedAnchorEffect]. Screens that want to align their own content with the feed read the
 * live, animated slide via [rememberCameraFeedOffsetFraction].
 */
@Stable
class CameraFeedController {
    /** The anchor the feed is currently sliding toward; set via [CameraFeedAnchorEffect]. */
    var targetAnchor by mutableStateOf(CameraFeedAnchor.FULL)
        internal set

    internal fun request(anchor: CameraFeedAnchor) {
        targetAnchor = anchor
    }
}

/**
 * Provides the [CameraFeedController] for the shared camera feed to the composition under
 * [CameraBackground]. Reading it outside that subtree throws.
 */
val LocalCameraFeedController = staticCompositionLocalOf<CameraFeedController> {
    error("No CameraFeedController provided; wrap content in CameraBackground")
}

/**
 * Declaratively requests [anchor] for the shared camera feed while this effect is in composition,
 * resetting the feed to [CameraFeedAnchor.FULL] when it leaves. Mirrors the
 * `BackHandler`/system-UI-controller pattern: any screen can drive the feed from its own scope.
 *
 * @param anchor the slide position to request while composed.
 */
@Composable
fun CameraFeedAnchorEffect(anchor: CameraFeedAnchor) {
    val controller = LocalCameraFeedController.current
    LaunchedEffect(anchor) { controller.request(anchor) }
    DisposableEffect(Unit) {
        onDispose { controller.request(CameraFeedAnchor.FULL) }
    }
}

/**
 * The shared camera feed's live slide fraction (`0f`..`1f`), animated with the canonical spec.
 * Screens read this to translate their own feed-attached content (e.g. buttons) in lock-step with
 * the feed: it animates the same target with the same spec as [CameraBackground], so both stay
 * frame-for-frame in sync and ease together.
 */
@Composable
fun rememberCameraFeedOffsetFraction(): Float {
    val controller = LocalCameraFeedController.current
    val fraction by animateFloatAsState(
        targetValue = controller.targetAnchor.offsetFraction,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )
    return fraction
}
