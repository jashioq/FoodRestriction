package com.jan.food.presentation.components.cutout.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.cutout.rememberCutoutOutlinePath
import com.jan.food.presentation.components.cutout.rememberDisplayCutout

/** Thickness of the travelling segment; centered on the cutout edge so half straddles each side. */
private val StrokeWidth = 10.dp

/** Segment color. */
private val BorderColor = Color.DarkGray

/** Fraction of the cutout circumference the segment covers. */
private const val SegmentFraction = 0.5f

/** Duration of one full lap around the cutout, in milliseconds. */
private const val DurationMillis = 1000

/** Travel direction. `true` advances along the path (clockwise); flip if it reads counter-clockwise. */
private const val Clockwise = false

/**
 * The loading animation: a [StrokeWidth]-thick segment covering [SegmentFraction] of the device's
 * display-cutout circumference, travelling around it in an endless loop. The stroke is centered on the
 * cutout edge, so half is obscured by the cutout and half spills outside it — masking small geometry
 * measurement errors. Fills the screen, draws nothing else, and does not intercept input.
 *
 * Continuity across the path's start/end seam is preserved with a dashed stroke whose pattern period
 * equals the full path length: exactly one "on" arc is ever visible, and when it runs off the path end
 * the periodic pattern places its continuation at the start, so it never changes length or snaps as it
 * spins. Animating the dash phase slides that arc around the loop.
 *
 * Enter/exit is owned by the caller and supplied as [progress] (0 = absent, 1 = fully shown): it scales
 * both alpha and travel speed, so the segment fades in while accelerating from a standstill and fades
 * out while coasting to a stop.
 *
 * @param progress the 0..1 enter/exit ramp from the host; the segment is invisible at 0.
 */
@Composable
fun Loading(progress: Float, modifier: Modifier = Modifier) {
    val cutout = rememberDisplayCutout()
    val outlinePath = rememberCutoutOutlinePath()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()

    // The cutout outline is a closed loop; the no-cutout fallback is an open top-edge line.
    val isClosed = cutout.hasClosedOutline(outlinePath)

    val path = remember(outlinePath, cutout, density, layoutDirection, screenWidthPx) {
        buildCutoutOutlinePath(outlinePath, cutout, density, layoutDirection, screenWidthPx)
    }
    val total = remember(path, isClosed) { PathMeasure().apply { setPath(path, isClosed) }.length }

    // Accumulated travel as a 0..1 fraction of the loop, integrated each frame at a speed scaled by
    // progress (so it accelerates from rest on enter and coasts to a stop on exit).
    val currentProgress by rememberUpdatedState(progress)
    var travel by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    val lapsPerSecond = currentProgress * (1000f / DurationMillis)
                    travel = (travel + lapsPerSecond * dt) % 1f
                }
                lastNanos = now
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (total <= 0f || progress <= 0f) return@Canvas

        // One "on" arc followed by a "gap" filling the rest of the loop. The pattern period equals
        // total, so only one arc shows; shifting the phase by total slides it once around. Negate for
        // clockwise (dash phase advances the pattern backwards).
        val segmentLength = total * SegmentFraction
        val dashPhase = (if (Clockwise) -1f else 1f) * travel * total
        val effect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(segmentLength, total - segmentLength),
            phase = dashPhase,
        )

        drawPath(
            path = path,
            color = BorderColor,
            alpha = progress,
            style = Stroke(width = StrokeWidth.toPx(), cap = StrokeCap.Round, pathEffect = effect),
        )
    }
}
