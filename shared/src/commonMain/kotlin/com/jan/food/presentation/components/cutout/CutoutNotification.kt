package com.jan.food.presentation.components.cutout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The animation the cutout notification plays around the device's display cutout.
 *
 * - [NONE] — nothing drawn.
 * - [LOADING] — a line half the cutout's circumference travelling around it in a loop.
 * - [ALERT] — two red light beams sweeping around the cutout like a lighthouse, 180° apart.
 * - [LOW_ALERT] — a yellow border pulsing around the cutout, with no beams.
 */
enum class CutoutNotificationAnimation {
    NONE,
    LOADING,
    ALERT,
    LOW_ALERT,
}

/**
 * Central control surface for the shared cutout notification hosted by [CutoutNotification]. Holds
 * the currently playing [animation]; the notification renders from it and screens drive it
 * declaratively with [CutoutNotificationEffect]. Obtain it from anywhere under the notification via
 * [LocalCutoutNotificationController].
 */
@Stable
class CutoutNotificationController {
    /** The animation currently playing around the cutout. */
    var animation by mutableStateOf(CutoutNotificationAnimation.NONE)
        internal set

    internal fun apply(animation: CutoutNotificationAnimation) {
        this.animation = animation
    }

    /** Restores the default, static (animation-free) notification. */
    internal fun reset() = apply(CutoutNotificationAnimation.NONE)
}

/**
 * Provides the [CutoutNotificationController] to the composition under [CutoutNotification]. Reading
 * it outside that subtree throws.
 */
val LocalCutoutNotificationController = staticCompositionLocalOf<CutoutNotificationController> {
    error("No CutoutNotificationController provided; wrap content in CutoutNotification")
}

/**
 * Declaratively selects the cutout notification's [animation] while this effect is in composition,
 * restoring [CutoutNotificationAnimation.NONE] when it leaves. Mirrors the
 * `BackHandler`/[com.jan.food.presentation.components.camera.CameraFeedEffect] pattern: any screen drives the notification from its own scope.
 *
 * @param animation the animation to play (default [CutoutNotificationAnimation.NONE]).
 */
@Composable
fun CutoutNotificationEffect(animation: CutoutNotificationAnimation = CutoutNotificationAnimation.NONE) {
    val controller = LocalCutoutNotificationController.current
    DisposableEffect(animation) {
        controller.apply(animation)
        onDispose { controller.reset() }
    }
}

/**
 * Wraps [content] and overlays the cutout notification (static border plus the controller's current
 * animation) on top of it, so the cutout outline is always visible above every screen. Place this
 * around the app's navigation host; screens drive it with [CutoutNotificationEffect].
 */
@Composable
fun CutoutNotification(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val controller = remember { CutoutNotificationController() }
    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalCutoutNotificationController provides controller) {
            content()
        }
        CutoutBorder(animation = controller.animation)
    }
}
