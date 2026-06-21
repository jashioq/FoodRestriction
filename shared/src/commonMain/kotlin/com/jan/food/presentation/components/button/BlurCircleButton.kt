package com.jan.food.presentation.components.button

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable circular "frosted darkening" button: a soft, borderless circle of [blurColor] that
 * sits behind [content] and fades smoothly into the background, so it has no visible edge.
 *
 * The circle is solid (at [blurColorAlpha]) out to [radius], then its alpha falls off from
 * [blurColorAlpha] to `0` across [blurFadeSize] so the color blends seamlessly into whatever is
 * behind it. [blurStrength] adds an extra blur over the whole circle for a frosted look; the
 * [content] stays sharp on top.
 *
 * @param onClick invoked when the button is tapped.
 * @param modifier the [Modifier] applied to the button.
 * @param radius the radius of the solid (un-faded) part of the circle.
 * @param blurStrength how much to blur the circle itself; `0.dp` relies purely on the fade.
 * @param blurColor the color of the circle.
 * @param blurColorAlpha the alpha of the solid part of the circle (`0f`..`1f`).
 * @param blurFadeSize how far the gradual fade extends beyond [radius]; over this band the alpha
 * transitions from [blurColorAlpha] down to `0`.
 * @param tapColor the color the circle eases to while pressed.
 * @param tapColorAlpha the alpha of the solid part of the circle while pressed (`0f`..`1f`).
 * @param tapAnimationMillis the total duration of one full tap animation (base → tap → base);
 * each half runs for half this value.
 * @param content the centered, sharp button content.
 */
@Composable
fun BlurCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    blurStrength: Dp = 8.dp,
    blurColor: Color = Color.Black,
    blurColorAlpha: Float = 0.4f,
    blurFadeSize: Dp = 12.dp,
    tapColor: Color = Color.White,
    tapColorAlpha: Float = 0.4f,
    tapAnimationMillis: Int = 350,
    content: @Composable () -> Unit,
) {
    // The tappable / layout footprint is the full circle including its fade band.
    val diameter = (radius + blurFadeSize) * 2

    val interactionSource = remember { MutableInteractionSource() }

    val baseColor = blurColor.copy(alpha = blurColorAlpha)
    val pressedColor = tapColor.copy(alpha = tapColorAlpha)

    // Drives the solid color manually so each press plays the full base → tap → base sequence
    // rather than reversing on release.
    val solidColorAnim = remember { Animatable(baseColor, Color.VectorConverter(baseColor.colorSpace)) }
    val solidColor = solidColorAnim.value

    // Replay the full sequence on every press, regardless of how long the press is held.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                val half = tapAnimationMillis / 2
                solidColorAnim.snapTo(baseColor)
                solidColorAnim.animateTo(
                    targetValue = pressedColor,
                    animationSpec = tween(durationMillis = half, easing = FastOutLinearInEasing),
                )
                solidColorAnim.animateTo(
                    targetValue = baseColor,
                    animationSpec = tween(durationMillis = half, easing = FastOutLinearInEasing),
                )
            }
        }
    }

    Box(
        modifier = modifier
            .size(diameter)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(blurStrength, BlurredEdgeTreatment.Unbounded)
                .drawBehind {
                    val gradientRadiusPx = radius.toPx() + blurFadeSize.toPx()
                    // Fraction of the gradient that stays fully solid before the fade starts.
                    val solidStop = (radius.toPx() / gradientRadiusPx).coerceIn(0f, 1f)
                    val solid = solidColor
                    val transparent = solidColor.copy(alpha = 0f)
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to solid,
                            solidStop to solid,
                            1f to transparent,
                            center = center,
                            radius = gradientRadiusPx,
                        ),
                        radius = gradientRadiusPx,
                        center = center,
                    )
                },
        )

        content()
    }
}
