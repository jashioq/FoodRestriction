package com.jan.food.presentation.components.cutout.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.cutout.rememberCutoutOutlinePath
import com.jan.food.presentation.components.cutout.rememberDisplayCutout
import kotlin.math.PI
import kotlin.math.cos

// --- Tunable controls -----------------------------------------------------------------------------

/** Border color. */
private val BorderColor = Color(0xFFFFC400)

/** Thickness of the pulsing cutout border; centered on the edge so it straddles the cutout, masking
 * minor measurement errors the way the loading border does. */
private val BorderThickness = 16.dp

/** Blur softening the border so it fades toward its edges into light. */
private val BorderBlur = 7.dp

/** Duration of one full brightness pulse, in milliseconds (constant speed). */
private const val PulseDurationMillis = 1500

/** Border alpha at the dim trough and bright peak of a pulse. */
private const val BorderMinAlpha = 0.1f
private const val BorderMaxAlpha = 1f

/**
 * The low-alert animation: a static [BorderColor] border hugging the cutout's full circumference,
 * blurred toward its edges and gently pulsing in brightness at a constant speed. Like the alert border
 * but yellow and with no sweeping beams. The centered stroke straddles the cutout edge, masking minor
 * geometry measurement errors. Fills the screen, draws nothing else, and does not intercept input.
 *
 * Enter/exit is owned by the caller and supplied as [progress] (0 = absent, 1 = fully shown), which
 * scales the overall brightness so it fades in and out.
 *
 * @param progress the 0..1 enter/exit ramp from the host; the border is invisible at 0.
 */
@Composable
fun LowAlert(progress: Float, modifier: Modifier = Modifier) {
    val cutout = rememberDisplayCutout()
    val outlinePath = rememberCutoutOutlinePath()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()

    val path = remember(outlinePath, cutout, density, layoutDirection, screenWidthPx) {
        buildCutoutOutlinePath(outlinePath, cutout, density, layoutDirection, screenWidthPx)
    }

    // Constant-speed pulse phase; read inside the draw so the layer redraws without recomposing.
    val transition = rememberInfiniteTransition(label = "lowAlertPulse")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PulseDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(modifier = modifier.fillMaxSize().blur(BorderBlur, BlurredEdgeTreatment.Unbounded)) {
        if (progress <= 0f) return@Canvas

        // Start at the trough (phase 0 = dim) so the first rise runs a full half-period and grows
        // in step with the host's enter ramp, instead of peaking instantly and getting clipped by it.
        val pulse = (0.5 - 0.5 * cos(2.0 * PI * phase.value)).toFloat()
        val alpha = ((BorderMinAlpha + (BorderMaxAlpha - BorderMinAlpha) * pulse) * progress)
            .coerceIn(0f, 1f)

        drawPath(
            path = path,
            color = BorderColor.copy(alpha = alpha),
            style = Stroke(width = BorderThickness.toPx(), cap = StrokeCap.Round),
        )
    }
}
