package com.jan.food.presentation.components.button

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/** Default solid color of a [BlurCircleButton]'s circle at rest. */
internal val DefaultPulseColor = Color.Black.copy(alpha = 0.4f)

/** Default solid color the circle eases to while pressed. */
internal val DefaultPulsePressedColor = Color.White.copy(alpha = 0.4f)

/** Default duration of one full base → tap → base pulse. */
internal const val DefaultPulseMillis = 350

/**
 * Hoistable color animation for a [BlurCircleButton]'s frosted circle. Share a single instance
 * across several buttons (e.g. a menu button and the back button it navigates to) and they will
 * pulse in perfect sync, since they all read the same [color]. Trigger a pulse with [pulse].
 *
 * A [BlurCircleButton] given no state falls back to its own private instance, so it animates
 * itself on press as usual.
 *
 * @param baseColor the circle color at rest.
 * @param pressedColor the circle color at the peak of a pulse.
 * @param pulseMillis the total duration of one base → peak → base pulse.
 */
@Stable
class TapPulseState(
    private val baseColor: Color,
    private val pressedColor: Color,
    private val pulseMillis: Int,
) {
    private val animatable = Animatable(baseColor, Color.VectorConverter(baseColor.colorSpace))

    /** The current circle color to render. */
    val color: Color get() = animatable.value

    /** Plays one full base → peak → base pulse, restarting from the base if one is already running. */
    suspend fun pulse() {
        val half = pulseMillis / 2
        animatable.snapTo(baseColor)
        animatable.animateTo(pressedColor, tween(durationMillis = half, easing = FastOutLinearInEasing))
        animatable.animateTo(baseColor, tween(durationMillis = half, easing = FastOutLinearInEasing))
    }
}

/**
 * Remembers a [TapPulseState] keyed on its colors and duration. Hoist this above the buttons that
 * should share a pulse and pass it to each one.
 */
@Composable
fun rememberTapPulseState(
    baseColor: Color = DefaultPulseColor,
    pressedColor: Color = DefaultPulsePressedColor,
    pulseMillis: Int = DefaultPulseMillis,
): TapPulseState = remember(baseColor, pressedColor, pulseMillis) {
    TapPulseState(baseColor, pressedColor, pulseMillis)
}
