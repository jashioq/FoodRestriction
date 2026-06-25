package com.jan.food.presentation.components.cutout.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.cutout.DisplayCutout
import com.jan.food.presentation.components.cutout.DisplayCutoutType
import com.jan.food.presentation.components.cutout.rememberCutoutOutlinePath
import com.jan.food.presentation.components.cutout.rememberDisplayCutout

/** Thickness of the travelling segment; centered on the cutout edge so half straddles each side. */
private val StrokeWidth = 10.dp

/** Segment color. */
private val BorderColor = Color.Blue

/** Bottom-corner rounding of the notch outline (the top edge sits flush with the screen edge). */
private val NotchCornerRadius = 18.dp

/** Fraction of the cutout circumference the segment covers at any instant. */
private const val SegmentFraction = 0.5f

/** Duration of one full lap around the cutout, in milliseconds. */
private const val DurationMillis = 1500

/** Travel direction. `true` advances along the path (clockwise); flip if it reads counter-clockwise. */
private const val Clockwise = false

/**
 * The loading animation: a [StrokeWidth]-thick segment covering [SegmentFraction] of the device's
 * display-cutout circumference, travelling around it in an endless loop. The stroke is centered on the
 * cutout edge, so half is obscured by the cutout and half spills outside it — masking small geometry
 * measurement errors. Fills the screen, draws nothing else, and does not intercept input.
 *
 * Continuity across the path's start/end seam is preserved with a dashed stroke whose pattern period
 * equals the full path length: exactly one [SegmentFraction] "on" arc is ever visible, and when it
 * runs off the path end the periodic pattern places its continuation at the start, so it never changes
 * length or snaps as it spins. Animating the dash phase slides that arc around the loop.
 */
@Composable
fun Loading(modifier: Modifier = Modifier) {
    val cutout = rememberDisplayCutout()
    val outlinePath = rememberCutoutOutlinePath()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()

    // The cutout outline is a closed loop; the no-cutout fallback is an open top-edge line.
    val isClosed = outlinePath != null || cutout.type != DisplayCutoutType.NO_CUTOUT

    val path = remember(outlinePath, cutout, density, layoutDirection, screenWidthPx) {
        buildCenterlinePath(outlinePath, cutout, density, layoutDirection, screenWidthPx)
    }
    val total = remember(path, isClosed) { PathMeasure().apply { setPath(path, isClosed) }.length }

    val transition = rememberInfiniteTransition(label = "cutoutLoading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (total <= 0f) return@Canvas

        // One "on" arc of segmentLength followed by a "gap" filling the rest of the loop. The pattern
        // period equals total, so only one arc shows; shifting the phase by total over the cycle
        // slides it once around. Negate for clockwise (dash phase advances the pattern backwards).
        val segmentLength = total * SegmentFraction
        val dashPhase = (if (Clockwise) -1f else 1f) * phase * total
        val effect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(segmentLength, total - segmentLength),
            phase = dashPhase,
        )

        drawPath(
            path = path,
            color = BorderColor,
            style = Stroke(width = StrokeWidth.toPx(), cap = StrokeCap.Round, pathEffect = effect),
        )
    }
}

/**
 * Builds the centerline [Path] the segment travels along, running directly on the cutout edge (the
 * centered stroke straddles it). Uses the platform's exact outline when available; otherwise derives
 * the outline from the per-type shape and cutout box, or an open top-edge line when there's no cutout.
 */
private fun buildCenterlinePath(
    outlinePath: Path?,
    cutout: DisplayCutout,
    density: Density,
    layoutDirection: LayoutDirection,
    screenWidthPx: Float,
): Path {
    if (outlinePath != null) return outlinePath

    if (cutout.type == DisplayCutoutType.NO_CUTOUT) {
        return Path().apply {
            moveTo(0f, 0f)
            lineTo(screenWidthPx, 0f)
        }
    }

    val shape = when (cutout.type) {
        // Punch-holes and the Dynamic Island read as fully-rounded capsules/circles.
        DisplayCutoutType.DYNAMIC_ISLAND, DisplayCutoutType.CUSTOM -> RoundedCornerShape(percent = 50)
        // Notches meet the screen's top edge squarely and round only at the bottom.
        DisplayCutoutType.LARGE_NOTCH, DisplayCutoutType.SMALL_NOTCH ->
            RoundedCornerShape(bottomStart = NotchCornerRadius, bottomEnd = NotchCornerRadius)

        DisplayCutoutType.NO_CUTOUT -> RoundedCornerShape(0.dp)
    }

    val sizePx = with(density) { Size(cutout.size.width.toPx(), cutout.size.height.toPx()) }
    val offsetPx = with(density) { Offset(cutout.offset.x.toPx(), cutout.offset.y.toPx()) }
    val outline = shape.createOutline(sizePx, layoutDirection, density)
    return Path().apply {
        addOutline(outline)
        translate(offsetPx)
    }
}
