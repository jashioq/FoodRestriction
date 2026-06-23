package com.jan.food.presentation.components.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Rows of cell content for [ReactiveGrid]; a `null` entry leaves a gap. Rows may differ in length. */
typealias ReactiveGridContent = List<List<(@Composable () -> Unit)?>>

/**
 * The tunable "magnet" feel of a [ReactiveGrid] tap: how far neighbours shove, how the push decays
 * and staggers outward, how much the tapped cell swells, and the spring that drives it all (speed +
 * bounce). The defaults are the shipped feel; override only what you want to change.
 *
 * @property pushStrength how far a directly-adjacent (ring 1) cell shoves away at the peak; outer
 * rings shove [falloff]^(ring-1) as far.
 * @property falloff per-ring multiplier applied to [pushStrength] (`0f`..`1f`); smaller decays faster.
 * @property pressScale how much the tapped cell grows at the peak (e.g. `0.3f` = +30%).
 * @property ringDelay how long each successive ring waits before it reacts, creating the
 * travelling-wave look.
 * @property pushStiffness spring stiffness for the neighbour push; higher is quicker.
 * @property pushDamping spring damping ratio for the neighbour push; lower bounces more.
 * @property swellStiffness spring stiffness for the tapped cell's swell; higher is quicker.
 * @property swellDamping spring damping ratio for the tapped cell's swell; lower bounces more.
 */
data class ReactiveGridAnimation(
    val pushStrength: Dp = 18.dp,
    val falloff: Float = 0.5f,
    val pressScale: Float = 0.3f,
    val ringDelay: Duration = 45.milliseconds,
    val pushStiffness: Float = 300f,
    val pushDamping: Float = 0.3f,
    val swellStiffness: Float = 350f,
    val swellDamping: Float = 0.35f,
)

/**
 * A grid that renders a 2-D list of composables as rows and columns and reacts to a tap with a
 * "magnet" ripple: the tapped cell grows a little while its neighbours shove outward to make room,
 * the next ring shoves a little less, and so on — each ring kicking in slightly after the one inside
 * it so the push visibly travels outward like repelling magnets.
 *
 * The reaction is driven purely from grid coordinates (no pixel measuring): every cell pushes
 * radially away from the tapped cell, with both the strength and the start delay scaled by its ring
 * distance. Displacement is applied via `graphicsLayer`, so cells overlap into each other's space
 * without disturbing the underlying layout.
 *
 * @param items rows of cell content; a `null` entry leaves a gap. Rows may have differing lengths.
 * @param onItemClick invoked with the `row` and `column` of the tapped cell.
 * @param modifier the [Modifier] applied to the grid.
 * @param horizontalSpacing the gap between columns at rest.
 * @param verticalSpacing the gap between rows at rest.
 * @param animation the magnet-ripple feel; see [ReactiveGridAnimation].
 */
@Composable
fun ReactiveGrid(
    items: ReactiveGridContent,
    onItemClick: (row: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 16.dp,
    verticalSpacing: Dp = 16.dp,
    animation: ReactiveGridAnimation = ReactiveGridAnimation(),
) {
    // The tapped cell plus a monotonically increasing id so re-tapping the same cell re-triggers.
    var pressedRow by remember { mutableStateOf(-1) }
    var pressedCol by remember { mutableStateOf(-1) }
    var pressId by remember { mutableStateOf(0) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEachIndexed { row, rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowItems.forEachIndexed { column, cell ->
                    if (cell != null) {
                        ReactiveGridCell(
                            row = row,
                            column = column,
                            pressedRow = pressedRow,
                            pressedCol = pressedCol,
                            pressId = pressId,
                            animation = animation,
                            onClick = {
                                pressedRow = row
                                pressedCol = column
                                pressId++
                                onItemClick(row, column)
                            },
                            content = cell,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single cell of [ReactiveGrid], modelled as a spring-loaded mass anchored at its home position
 * (and home scale `1`). A tap doesn't move the cell to a target and back — it injects an outward
 * *impulse* (a velocity kick) and lets a single under-damped spring carry the cell out and straight
 * back home in one continuous motion, so there's no dwell at the peak. Since the kick is *added* to
 * the cell's current velocity and `animateTo` resumes from its current value, overlapping taps
 * accumulate like real impulses rather than snapping.
 */
@Composable
private fun ReactiveGridCell(
    row: Int,
    column: Int,
    pressedRow: Int,
    pressedCol: Int,
    pressId: Int,
    animation: ReactiveGridAnimation,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val translation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scale = remember { Animatable(1f) }
    val density = LocalDensity.current
    val isPressed = row == pressedRow && column == pressedCol

    // Radial direction away from the tapped cell (euclidean) for a natural push...
    val dr = (row - pressedRow).toFloat()
    val dc = (column - pressedCol).toFloat()
    val distance = sqrt(dr * dr + dc * dc)
    val dirX = if (distance == 0f) 0f else dc / distance
    val dirY = if (distance == 0f) 0f else dr / distance

    // ...but concentric (chebyshev) rings for the strength tier and the stagger delay.
    val ring = max(abs(row - pressedRow), abs(column - pressedCol))
    val magnitude = animation.pushStrength * animation.falloff.pow((ring - 1).coerceAtLeast(0))

    // Springs anchored at home, derived from the config. The kick is scaled by sqrt(stiffness) so an
    // impulse's peak (≈ velocity / sqrt(stiffness)) tracks the intended push distance / swell.
    val translationSpec = spring<Offset>(animation.pushDamping, animation.pushStiffness)
    val scaleSpec = spring<Float>(animation.swellDamping, animation.swellStiffness)
    val pushVelocityFactor = sqrt(animation.pushStiffness)
    val swellVelocityFactor = sqrt(animation.swellStiffness)

    LaunchedEffect(pressId) {
        if (pressId == 0) return@LaunchedEffect // No tap yet.
        coroutineScope {
            // Scale: kick the tapped cell upward; the spring carries it up and back to size 1 in
            // one motion. Neighbours just spring whatever residual scale they have back to rest.
            launch {
                val kick = if (isPressed) animation.pressScale * swellVelocityFactor else 0f
                scale.animateTo(1f, scaleSpec, initialVelocity = scale.velocity + kick)
            }
            // Translation: neighbours get an outward velocity kick (after their ring delay) and the
            // spring carries them out and straight back home. The tapped cell stays put.
            launch {
                val kick = if (isPressed) {
                    Offset.Zero
                } else {
                    delay(animation.ringDelay * ring)
                    val magnitudePx = with(density) { magnitude.toPx() }
                    Offset(dirX, dirY) * (magnitudePx * pushVelocityFactor)
                }
                translation.animateTo(Offset.Zero, translationSpec, initialVelocity = translation.velocity + kick)
            }
        }
    }

    Box(
        modifier = Modifier
            // Keep the growing tapped cell above its retreating neighbours.
            .zIndex(if (isPressed) 1f else 0f)
            .graphicsLayer {
                translationX = translation.value.x
                translationY = translation.value.y
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
    }
}
