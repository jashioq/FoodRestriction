package com.jan.food.presentation.components.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

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
 * @param pushStrength how far a directly-adjacent (ring 1) cell shoves away at the peak of the
 * animation; outer rings shove [falloff]^(ring-1) as far.
 * @param falloff per-ring multiplier applied to [pushStrength] (`0f`..`1f`); smaller decays faster.
 * @param pressScale how much the tapped cell grows at the peak (e.g. `0.15f` = +15%).
 * @param ringDelayMillis how long each successive ring waits before it reacts, creating the
 * travelling-wave look.
 */
@Composable
fun ReactiveGrid(
    items: List<List<(@Composable () -> Unit)?>>,
    onItemClick: (row: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 16.dp,
    verticalSpacing: Dp = 16.dp,
    pushStrength: Dp = 18.dp,
    falloff: Float = 0.5f,
    pressScale: Float = 0.15f,
    ringDelayMillis: Int = 45,
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
                            pushStrength = pushStrength,
                            falloff = falloff,
                            pressScale = pressScale,
                            ringDelayMillis = ringDelayMillis,
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
 * A single cell of [ReactiveGrid]. Owns its own `0 → 1 → 0` push [Animatable] and replays it on
 * every [pressId] change, after a ring-distance delay so outer rings trail inner ones.
 */
@Composable
private fun ReactiveGridCell(
    row: Int,
    column: Int,
    pressedRow: Int,
    pressedCol: Int,
    pressId: Int,
    pushStrength: Dp,
    falloff: Float,
    pressScale: Float,
    ringDelayMillis: Int,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val push = remember { Animatable(0f) }
    val isPressed = row == pressedRow && column == pressedCol

    // Radial direction away from the tapped cell (euclidean) for a natural push...
    val dr = (row - pressedRow).toFloat()
    val dc = (column - pressedCol).toFloat()
    val distance = sqrt(dr * dr + dc * dc)
    val dirX = if (distance == 0f) 0f else dc / distance
    val dirY = if (distance == 0f) 0f else dr / distance

    // ...but concentric (chebyshev) rings for the strength tier and the stagger delay.
    val ring = max(abs(row - pressedRow), abs(column - pressedCol))
    val magnitude = pushStrength * falloff.pow((ring - 1).coerceAtLeast(0))

    LaunchedEffect(pressId) {
        if (pressId == 0) return@LaunchedEffect // No tap yet.
        delay(ring.toLong() * ringDelayMillis)
        push.snapTo(0f)
        push.animateTo(1f, tween(durationMillis = 140, easing = FastOutSlowInEasing))
        // A springy return gives the cells their magnet-like rebound back into place.
        push.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier
            // Keep the growing tapped cell above its retreating neighbours.
            .zIndex(if (isPressed) 1f else 0f)
            .graphicsLayer {
                val p = push.value
                if (isPressed) {
                    val scale = 1f + pressScale * p
                    scaleX = scale
                    scaleY = scale
                } else {
                    translationX = dirX * magnitude.toPx() * p
                    translationY = dirY * magnitude.toPx() * p
                }
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
