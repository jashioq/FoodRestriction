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

/** Duration of the shared camera feed's slide/corner/darken animations. */
internal const val CameraFeedSlideMillis = 500

/** Easing of the shared camera feed's slide/corner/darken animations. */
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
 * Central control surface for the single shared camera feed hosted by [CameraBackground]. Holds
 * every adjustable feed property; [CameraBackground] renders from it and screens drive it
 * declaratively with [CameraFeedEffect]. Obtain it from anywhere under the feed via
 * [LocalCameraFeedController], or read the live slide via [rememberCameraFeedOffsetFraction].
 */
@Stable
class CameraFeedController {
    /** The anchor the feed is currently sliding toward. */
    var anchor by mutableStateOf(CameraFeedAnchor.FULL)
        internal set

    /** When true, the feed renders blurred (natively, per platform). */
    var blurred by mutableStateOf(false)
        internal set

    /** When true, tapping the feed triggers tap-to-focus (Android only, and only while [FULL]). */
    var tapToFocus by mutableStateOf(true)
        internal set

    /** When true, a translucent black scrim dims the feed. */
    var darken by mutableStateOf(false)
        internal set

    internal fun apply(
        anchor: CameraFeedAnchor,
        blurred: Boolean,
        tapToFocus: Boolean,
        darken: Boolean,
    ) {
        this.anchor = anchor
        this.blurred = blurred
        this.tapToFocus = tapToFocus
        this.darken = darken
    }

    /** Restores the default, full-screen, sharp, focusable, undimmed feed. */
    internal fun reset() = apply(CameraFeedAnchor.FULL, blurred = false, tapToFocus = true, darken = false)
}

/**
 * Provides the [CameraFeedController] for the shared camera feed to the composition under
 * [CameraBackground]. Reading it outside that subtree throws.
 */
val LocalCameraFeedController = staticCompositionLocalOf<CameraFeedController> {
    error("No CameraFeedController provided; wrap content in CameraBackground")
}

/**
 * Declaratively configures the shared camera feed while this effect is in composition, restoring the
 * defaults when it leaves. Mirrors the `BackHandler`/system-UI-controller pattern: any screen drives
 * the feed from its own scope, and only the controlling (composed) screen's settings apply.
 *
 * @param anchor the feed slide position (default [CameraFeedAnchor.FULL]).
 * @param blurred whether the feed renders blurred (default `false`).
 * @param tapToFocus whether tap-to-focus is enabled (default `true`).
 * @param darken whether a translucent black scrim dims the feed (default `false`).
 */
@Composable
fun CameraFeedEffect(
    anchor: CameraFeedAnchor = CameraFeedAnchor.FULL,
    blurred: Boolean = false,
    tapToFocus: Boolean = true,
    darken: Boolean = false,
) {
    val controller = LocalCameraFeedController.current
    LaunchedEffect(anchor, blurred, tapToFocus, darken) {
        controller.apply(anchor, blurred, tapToFocus, darken)
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
        targetValue = controller.anchor.offsetFraction,
        animationSpec = tween(CameraFeedSlideMillis, easing = CameraFeedSlideEasing),
    )
    return fraction
}
