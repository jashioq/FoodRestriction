package com.jan.food.presentation.components.cutout.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.cutout.DisplayCutoutType
import com.jan.food.presentation.components.cutout.rememberCutoutOutlinePath
import com.jan.food.presentation.components.cutout.rememberDisplayCutout
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

// --- Tunable controls -----------------------------------------------------------------------------

/** Beam color toward the far end of the beam. */
private val BeamColor = Color.Red

/** Beam color at the apex — a brighter, pink-ish red that blends into [BeamColor] along the length. */
private val BeamNearColor = Color(0xFFFF5C7A)

/**
 * Light intensity: scales brightness (alpha) and color saturation (a dim beam washes toward grey),
 * independently of size. The live value is this ramped by the host `progress`.
 */
private const val Intensity = 1f

/** Lens spread: the full angular width of each beam's cone, in degrees. Smaller = a narrower beam. */
private const val SpreadDegrees = 44f

/** Beam size in stage one: how far each beam reaches. Scales length (and, via [SpreadDegrees], width)
 * independently of [Intensity], so the beams can be larger or smaller without changing their brightness. */
private val BeamLength = 300.dp

/** Beam size in stage two, reached after [StageOneDurationMillis]. */
private val StageTwoLength = 120.dp

/** How long stage one lasts before easing down to the stage-two size and sweep speed. */
private const val StageOneDurationMillis = 5000

/** Duration of the eased stage-one → stage-two transition (size and sweep speed). */
private const val StageTransitionMillis = 500

/** Thickness of the pulsing cutout border; centered on the edge so it straddles the cutout, masking
 * minor measurement errors the way the loading border does. */
private val BorderThickness = 12.dp

/** Blur softening the border so it fades toward its edges into light. */
private val BorderBlur = 7.dp

/** Brightness pulses per full beam rotation; synced to the sweep so it slows with it across stages. */
private const val BorderPulsesPerLap = 2f

/** Border alpha at the dim trough and bright peak of a pulse (in stage one). */
private const val BorderMinAlpha = 0.3f
private const val BorderMaxAlpha = 1f

/** Border thickness in stage two — thinner than [BorderThickness]. */
private val StageTwoBorderThickness = 7.dp

/** Border brightness multiplier in stage two — dimmer than stage one's 1.0. */
private const val StageTwoBorderIntensity = 0.7f

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

/** Duration of one full sweep around the cutout in stage one, in milliseconds. */
private const val DurationMillis = 1000

/** Duration of one full sweep in stage two, in milliseconds (slower). */
private const val StageTwoDurationMillis = 2000

/** Sweep direction. `true` rotates clockwise; flip if it reads counter-clockwise. */
private const val Clockwise = true

/** Peak alpha of a beam at full intensity (kept below 1 so overlaps glow rather than clip). */
private const val MaxAlpha = 0.95f

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

    // Stage factor (0 = stage one, 1 = stage two), eased once after stage one ends. Every stage-two
    // change — beam size, sweep speed, border size and brightness — is derived from it, so they stay
    // in sync and transition without snapping.
    val stage = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(StageOneDurationMillis.toLong())
        stage.animateTo(1f, tween(durationMillis = StageTransitionMillis, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    val lapMillis = lerp(DurationMillis.toFloat(), StageTwoDurationMillis.toFloat(), stage.value)
                    val turnsPerSecond = currentProgress * (1000f / lapMillis)
                    sweep.floatValue = (sweep.floatValue + turnsPerSecond * dt) % 1f
                }
                lastNanos = now
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Each band is its own blurred layer; stacked apex-to-tip they form the distance-graded blur.
        for (band in BeamBands) {
            BeamBandCanvas(
                center = center,
                sweep = sweep,
                band = band,
                progress = { currentProgress },
                beamLengthDp = { lerp(BeamLength.value, StageTwoLength.value, stage.value) },
            )
        }
        // The static pulsing border hugging the cutout edge, on top of the beams.
        PulsingBorder(sweep = sweep, progress = { currentProgress }, stage = { stage.value })
    }
}

/**
 * A static border hugging the cutout's full circumference in [BeamColor]: a centered stroke (so it
 * straddles the cutout edge, masking minor measurement errors) blurred toward its edges, pulsing
 * between [BorderMinAlpha] and [BorderMaxAlpha] in sync with the beam sweep ([sweep]) so it slows
 * alongside the rotation across stages. Does not spin.
 *
 * In stage two ([stage] → 1) it grows dimmer ([StageTwoBorderIntensity]) and thinner
 * ([StageTwoBorderThickness]) alongside the shrinking beams.
 *
 * @param sweep the shared 0..1 sweep fraction driving the pulse phase.
 * @param progress the host enter/exit ramp; the border is invisible at 0.
 * @param stage the 0..1 stage factor (0 = stage one, 1 = stage two).
 */
@Composable
private fun PulsingBorder(sweep: FloatState, progress: () -> Float, stage: () -> Float) {
    val cutout = rememberDisplayCutout()
    val outlinePath = rememberCutoutOutlinePath()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()

    val path = remember(outlinePath, cutout, density, layoutDirection, screenWidthPx) {
        buildCutoutOutlinePath(outlinePath, cutout, density, layoutDirection, screenWidthPx)
    }

    Canvas(modifier = Modifier.fillMaxSize().blur(BorderBlur, BlurredEdgeTreatment.Unbounded)) {
        val p = progress()
        if (p <= 0f) return@Canvas
        val s = stage()

        // Pulse phase rides the sweep, so it speeds up / slows down exactly with the rotation. Starts at
        // the trough (sweep 0 = dim) so the first rise grows in step with the host's enter ramp instead
        // of peaking instantly and getting clipped by it.
        val pulse = (0.5 - 0.5 * cos(2.0 * PI * sweep.floatValue * BorderPulsesPerLap)).toFloat()
        val stageIntensity = lerp(1f, StageTwoBorderIntensity, s)
        val alpha = ((BorderMinAlpha + (BorderMaxAlpha - BorderMinAlpha) * pulse) * Intensity * p * stageIntensity)
            .coerceIn(0f, 1f)
        val thickness = lerp(BorderThickness.value, StageTwoBorderThickness.value, s).dp

        drawPath(
            path = path,
            color = BeamColor.copy(alpha = alpha),
            style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
        )
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
    beamLengthDp: () -> Float,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(band.blur, BlurredEdgeTreatment.Unbounded)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val p = progress()
        if (p <= 0f) return@Canvas

        // Size is independent of intensity: it comes from the staged length, only brightness/saturation
        // ramp with p.
        val intensity = Intensity * p
        val reach = beamLengthDp().dp.toPx()
        if (reach <= 0f) return@Canvas

        // Brightness via alpha, saturation by blending toward grey as intensity falls. Two endpoint
        // colors — pink-ish near the apex, red toward the end — interpolated along the beam length.
        val alpha = (intensity * MaxAlpha).coerceIn(0f, 1f)
        val sat = intensity.coerceIn(0f, 1f)
        val peakNear = lerp(WashedColor, BeamNearColor, sat).copy(alpha = alpha)
        val peakFar = lerp(WashedColor, BeamColor, sat).copy(alpha = alpha)

        val baseAngle = (if (Clockwise) 1f else -1f) * sweep.floatValue * 360f
        // Two lamps 180° apart.
        for (beam in 0..1) {
            drawBeam(center, baseAngle + beam * 180f, SpreadDegrees, reach, peakNear, peakFar, band.distanceStops)
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
    nearColor: Color,
    farColor: Color,
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
                    val pos = distanceStops[i].first
                    // Hue shifts from near to far along the length; alpha carries the distance falloff.
                    val hue = lerp(nearColor, farColor, pos)
                    pos to hue.copy(alpha = hue.alpha * distanceStops[i].second)
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
