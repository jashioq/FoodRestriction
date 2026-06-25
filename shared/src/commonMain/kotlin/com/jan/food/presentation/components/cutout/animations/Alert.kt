package com.jan.food.presentation.components.cutout.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.cutout.DisplayCutoutType
import com.jan.food.presentation.components.cutout.rememberDisplayCutout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

// --- Tunable controls -----------------------------------------------------------------------------

/** Beam color. */
private val BeamColor = Color.Red

/**
 * Light intensity: scales brightness (alpha), color saturation (a dim beam washes toward grey) and
 * the [MaxReach] distance the beam carries. The live value is this ramped by the host `progress`.
 */
private const val Intensity = 1f

/** Lens spread: the full angular width of each beam's cone, in degrees. Smaller = a narrower beam. */
private const val SpreadDegrees = 44f

/** Farthest distance a beam reaches at full [Intensity]. */
private val MaxReach = 280.dp

// --- Internal tuning ------------------------------------------------------------------------------

/**
 * A slice of the beam along its length, blurred on its own. Splitting the beam into bands whose
 * [distanceStops] sum to 1 at every radius (so the total intensity is unchanged) and blurring each more
 * the farther it sits lets the blur grow toward the end, imitating how light diffuses with distance.
 *
 * @param distanceStops alpha (0..1) by fraction along the beam, isolating this band's stretch.
 * @param blur the blur radius applied to this band.
 */
private class BeamBand(val distanceStops: Array<Pair<Float, Float>>, val blur: Dp)

/** Beam bands from apex to tip; near bands are crisp, far bands increasingly diffuse. */
private val BeamBands = listOf(
    BeamBand(
        distanceStops = arrayOf(0f to 1f, 0.25f to 0.7f, 0.5f to 0.3f, 0.75f to 0.05f, 1f to 0f),
        blur = 9.dp,
    ),
    BeamBand(
        distanceStops = arrayOf(0f to 0f, 0.25f to 0.25f, 0.5f to 0.4f, 0.75f to 0.25f, 1f to 0f),
        blur = 26.dp,
    ),
    BeamBand(
        distanceStops = arrayOf(0f to 0f, 0.25f to 0.05f, 0.5f to 0.3f, 0.75f to 0.7f, 1f to 1f),
        blur = 52.dp,
    ),
)

/** Duration of one full sweep around the cutout, in milliseconds. */
private const val DurationMillis = 2200

/** Sweep direction. `true` rotates clockwise; flip if it reads counter-clockwise. */
private const val Clockwise = true

/** Peak alpha of a beam at full intensity (kept below 1 so overlaps glow rather than clip). */
private const val MaxAlpha = 0.85f

/** A fully desaturated stand-in for [BeamColor], blended in as intensity drops. */
private val WashedColor = Color(0xFFB0A0A0)

/**
 * The alert animation: two red "light beams" sweeping around the device's display cutout like a
 * lighthouse with two lamps 180° apart. Each beam is a soft cone — a radial gradient fading with
 * distance, masked into shape — and is split into [BeamBands] so the blur grows toward the tip, like
 * light diffusing with distance. Fills the screen, draws nothing else, and does not intercept input.
 *
 * Two controls shape the look ([Intensity] and [SpreadDegrees]); see their docs. Enter/exit is owned
 * by the caller and supplied as [progress] (0 = absent, 1 = fully shown): it eases [Intensity] from 0
 * to its designated value and ramps the sweep speed from a standstill to full — reversed on the way out.
 *
 * @param progress the 0..1 enter/exit ramp from the host; the beams are invisible at 0.
 */
@Composable
fun Alert(progress: Float, modifier: Modifier = Modifier) {
    val cutout = rememberDisplayCutout()
    val density = LocalDensity.current
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()

    // The lighthouse pivot: the cutout's center, or the top-center of the screen when there's no cutout.
    val center = remember(cutout, density, screenWidthPx) {
        if (cutout.type == DisplayCutoutType.NO_CUTOUT) {
            Offset(screenWidthPx / 2f, 0f)
        } else with(density) {
            Offset(
                (cutout.offset.x + cutout.size.width / 2).toPx(),
                (cutout.offset.y + cutout.size.height / 2).toPx(),
            )
        }
    }

    // Accumulated sweep as a 0..1 fraction of a full turn, integrated each frame at a speed scaled by
    // progress (so it accelerates from rest on enter and coasts to a stop on exit). Held as state and
    // read inside each band's draw, so the bands stay in sync without recomposing.
    val currentProgress by rememberUpdatedState(progress)
    val sweep = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    val turnsPerSecond = currentProgress * (1000f / DurationMillis)
                    sweep.floatValue = (sweep.floatValue + turnsPerSecond * dt) % 1f
                }
                lastNanos = now
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Each band is its own blurred layer; stacked apex-to-tip they form the distance-graded blur.
        for (band in BeamBands) {
            BeamBandCanvas(center = center, sweep = sweep, band = band, progress = { currentProgress })
        }
    }
}

/**
 * Renders one [band] of both lamps into its own [band].blur layer. The offscreen layer (innermost)
 * isolates the beams so the DstIn alpha masks composite against the beams rather than the screen; the
 * blur (outermost) then diffuses this band. [progress] and [sweep] are read inside the draw so the
 * layer redraws each frame without recomposing.
 */
@Composable
private fun BeamBandCanvas(
    center: Offset,
    sweep: FloatState,
    band: BeamBand,
    progress: () -> Float,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(band.blur, BlurredEdgeTreatment.Unbounded)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val p = progress()
        if (p <= 0f) return@Canvas

        val intensity = Intensity * p
        val reach = MaxReach.toPx() * intensity
        if (reach <= 0f) return@Canvas

        // Brightness via alpha, saturation by blending toward grey as intensity falls.
        val saturated = lerp(WashedColor, BeamColor, intensity.coerceIn(0f, 1f))
        val peak = saturated.copy(alpha = (intensity * MaxAlpha).coerceIn(0f, 1f))

        val baseAngle = (if (Clockwise) 1f else -1f) * sweep.floatValue * 360f
        // Two lamps 180° apart.
        for (beam in 0..1) {
            drawBeam(center, baseAngle + beam * 180f, SpreadDegrees, reach, peak, band.distanceStops)
        }
    }
}

/** Sharp cross-section (alpha by fraction across the width): bright axis, dark edges — the apex look. */
private val SharpBellStops = arrayOf(
    0f to 0f,
    0.2f to 0.06f,
    0.35f to 0.35f,
    0.5f to 1f,
    0.65f to 0.35f,
    0.8f to 0.06f,
    1f to 0f,
)

/** Flat cross-section: broad and near-uniform with only the very edges tapering — the far-end look. */
private val FlatBellStops = arrayOf(
    0f to 0f,
    0.12f to 0.85f,
    0.5f to 1f,
    0.88f to 0.85f,
    1f to 0f,
)

/**
 * Distance weight (alpha by fraction along the beam) for the sharp section: it carries the whole beam
 * near the apex and crossfades out toward the end. Equals the beam's distance falloff times (1 − flat
 * weight).
 */
private val SharpDistanceStops = arrayOf(
    0f to 1f,
    0.2f to 0.8f,
    0.4f to 0.6f,
    0.6f to 0.2f,
    0.8f to 0f,
    1f to 0f,
)

/**
 * Distance weight for the flat section: zero near the apex, fading in over the far half. Together with
 * [SharpDistanceStops] the two sum to the beam's distance falloff at every radius, so the on-axis
 * intensity is unchanged (no bright transverse band) while the edges fill in toward the end.
 */
private val FlatDistanceStops = arrayOf(
    0f to 0f,
    0.4f to 0f,
    0.6f to 0.2f,
    0.8f to 0.2f,
    1f to 0f,
)

/**
 * Draws one light cone centered on [centerAngleDeg], emanating from [center] out to [reach]. It's a
 * triangle (apex at [center], base [spreadDeg] wide at the rim) built from two cross-sections that
 * crossfade along the length:
 *
 *  - a **sharp** section ([SharpBellStops]) — bright axis, dark edges — dominant near the narrow apex;
 *  - a **flat** section ([FlatBellStops]) — broad and near-uniform — fading in toward the wide end.
 *
 * Their distance weights ([SharpDistanceStops] + [FlatDistanceStops]) sum to the beam's falloff at every
 * radius, so the axis intensity is unchanged (no bright band) while the end evens out toward a shared
 * intensity. Each section is a radial gradient masked across its width by a perpendicular linear gradient
 * via [BlendMode.DstIn]; the flat section is shaped in its own layer and added on.
 *
 * Finally the assembled beam is multiplied by [bandStops] (a radial alpha envelope) so only this band's
 * stretch along the length remains — the caller blurs each band differently to grade the blur with
 * distance. The result reads as soft light with no bands or seams.
 */
private fun DrawScope.drawBeam(
    center: Offset,
    centerAngleDeg: Float,
    spreadDeg: Float,
    reach: Float,
    color: Color,
    bandStops: Array<Pair<Float, Float>>,
) {
    val angle = centerAngleDeg.toRadians()
    val halfWidth = reach * tan((spreadDeg / 2f).toRadians())
    val axisX = cos(angle)
    val axisY = sin(angle)
    // The rim's two corners, offset from the rim center along the perpendicular (-axisY, axisX).
    val rimX = center.x + axisX * reach
    val rimY = center.y + axisY * reach
    val edge1 = Offset(rimX - axisY * halfWidth, rimY + axisX * halfWidth)
    val edge2 = Offset(rimX + axisY * halfWidth, rimY - axisX * halfWidth)

    val triangle = Path().apply {
        moveTo(center.x, center.y)
        lineTo(edge1.x, edge1.y)
        lineTo(edge2.x, edge2.y)
        close()
    }

    fun section(distanceStops: Array<Pair<Float, Float>>, bellStops: Array<Pair<Float, Float>>) {
        drawPath(
            path = triangle,
            brush = Brush.radialGradient(
                colorStops = Array(distanceStops.size) { i ->
                    distanceStops[i].first to color.copy(alpha = color.alpha * distanceStops[i].second)
                },
                center = center,
                radius = reach,
            ),
        )
        drawPath(triangle, bellMask(edge1, edge2, bellStops), blendMode = BlendMode.DstIn)
    }

    // Sharp section drawn straight in; flat section shaped in its own layer then added on top.
    section(SharpDistanceStops, SharpBellStops)

    val bounds = Rect(
        left = minOf(center.x, edge1.x, edge2.x),
        top = minOf(center.y, edge1.y, edge2.y),
        right = maxOf(center.x, edge1.x, edge2.x),
        bottom = maxOf(center.y, edge1.y, edge2.y),
    )
    drawContext.canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.Plus })
    section(FlatDistanceStops, FlatBellStops)
    drawContext.canvas.restore()

    // Keep only this band's stretch along the length, so the caller can blur it independently.
    drawPath(
        path = triangle,
        brush = Brush.radialGradient(
            colorStops = Array(bandStops.size) { i ->
                bandStops[i].first to Color.Black.copy(alpha = bandStops[i].second)
            },
            center = center,
            radius = reach,
        ),
        blendMode = BlendMode.DstIn,
    )
}

/**
 * A perpendicular linear-gradient alpha mask spanning [edge1]..[edge2], for use with [BlendMode.DstIn]
 * (which multiplies the destination's alpha). [stops] give the cross-section alpha by fraction across
 * the width; only alpha matters, so an opaque black is tinted to each stop's level.
 */
private fun bellMask(edge1: Offset, edge2: Offset, stops: Array<Pair<Float, Float>>): Brush =
    Brush.linearGradient(
        colorStops = Array(stops.size) { i -> stops[i].first to Color.Black.copy(alpha = stops[i].second) },
        start = edge1,
        end = edge2,
    )

private fun Float.toRadians(): Float = this * (PI / 180f).toFloat()
