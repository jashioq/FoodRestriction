package com.jan.food.presentation.components.cutout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jan.food.presentation.components.cutout.animations.Loading

/** Duration of every animation's enter (and exit) transition, in milliseconds. */
private const val EnterExitMillis = 1000

/**
 * Renders the cutout notification's current [animation] around the device's display cutout. Fills the
 * screen and is meant to be overlaid on top of all other content via [CutoutNotification]; it draws
 * nothing else and does not intercept input.
 *
 * Visibility and the enter/exit transition live here: each animation is hosted via [CutoutAnimation],
 * which feeds it a single 0..1 `progress` ramp. The animation composables themselves are stateless
 * renderers of that progress, so adding a new one is just another [CutoutAnimation] call.
 * [CutoutNotificationAnimation.NONE] draws nothing.
 */
@Composable
fun CutoutBorder(
    modifier: Modifier = Modifier,
    animation: CutoutNotificationAnimation,
) {
    Box(modifier = modifier.fillMaxSize()) {
        CutoutAnimation(visible = animation == CutoutNotificationAnimation.LOADING) { progress ->
            Loading(progress = progress)
        }
    }
}

/**
 * Hosts a single cutout animation, owning its enter/exit transition. Drives a 0..1 `progress` ramp
 * (0 = absent, 1 = fully shown) toward [visible] over [EnterExitMillis], keeps [content] composed for
 * the whole exit, and drops it once `progress` reaches 0 — so idle animations cost nothing.
 *
 * @param visible whether the hosted animation should be shown; toggling it runs the ramp both ways.
 * @param content the stateless animation renderer, given the current `progress`.
 */
@Composable
private fun CutoutAnimation(
    visible: Boolean,
    content: @Composable (progress: Float) -> Unit,
) {
    var present by remember { mutableStateOf(visible) }
    if (visible) present = true
    if (!present) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = EnterExitMillis, easing = FastOutSlowInEasing),
        )
        if (!visible) present = false
    }
    content(progress.value)
}
