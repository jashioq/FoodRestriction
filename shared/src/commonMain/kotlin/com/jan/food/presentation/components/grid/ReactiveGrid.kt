package com.jan.food.presentation.components.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
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
 * The tunable "magnet" feel of a [ReactiveGrid] press: how far neighbours shove and hold, how that
 * push decays and staggers outward, how much the held cell swells, and the spring that drives the
 * expansion and the return (speed + bounce). The defaults are the shipped feel; override only what
 * you want to change.
 *
 * @property pushStrength how far a directly-adjacent (ring 1) cell shoves away while held; outer
 * rings shove [falloff]^(ring-1) as far.
 * @property falloff per-ring multiplier applied to [pushStrength] (`0f`..`1f`); smaller decays faster.
 * @property pressScale how much the held cell grows while pressed (e.g. `0.3f` = +30%).
 * @property ringDelay how long each successive ring waits before it shoves out, creating the
 * travelling-wave look as the grid expands.
 * @property minPressHold the shortest time a press stays expanded before it may return. Even a quick
 * tap holds this long, so roughly a third of the expansion always plays — a visible hint that the
 * pills are reactive.
 * @property pushStiffness spring stiffness for the neighbour push; higher is quicker.
 * @property pushDamping spring damping ratio for the neighbour push; lower bounces more.
 * @property swellStiffness spring stiffness for the held cell's swell; higher is quicker.
 * @property swellDamping spring damping ratio for the held cell's swell; lower bounces more.
 */
data class ReactiveGridAnimation(
    val pushStrength: Dp = 18.dp,
    val falloff: Float = 0.5f,
    val pressScale: Float = 0.3f,
    val ringDelay: Duration = 45.milliseconds,
    val minPressHold: Duration = 110.milliseconds,
    val pushStiffness: Float = 300f,
    val pushDamping: Float = 0.3f,
    val swellStiffness: Float = 350f,
    val swellDamping: Float = 0.35f,
)

/**
 * A grid that renders a 2-D list of composables as rows and columns and reacts to a *press-and-hold*
 * with a "magnet" expansion: on touch-down the pressed cell grows while its neighbours shove
 * outward — the next ring shoves a little less and slightly later, so the push visibly travels
 * outward like repelling magnets — and the whole grid **holds** that expanded shape until the finger
 * lifts. On release the cell and its neighbours spring back home and [onItemClick] fires.
 *
 * Lifting before the expansion settles simply reverses the springs from wherever they are, so a
 * quick tap eases back smoothly. Sliding the finger off the pressed cell before releasing cancels:
 * the grid returns home but [onItemClick] does **not** fire, letting the user back out of a press.
 *
 * The reaction is driven purely from grid coordinates (no pixel measuring): every cell pushes
 * radially away from the pressed cell, with both the strength and the start delay scaled by its ring
 * distance. Displacement is applied via `graphicsLayer`, so cells overlap into each other's space
 * without disturbing the underlying layout.
 *
 * @param items rows of cell content; a `null` entry leaves a gap. Rows may have differing lengths.
 * @param onItemClick invoked with the `row` and `column` of a cell when a press completes on it.
 * @param modifier the [Modifier] applied to the grid.
 * @param horizontalSpacing the gap between columns at rest.
 * @param verticalSpacing the gap between rows at rest.
 * @param animation the magnet feel; see [ReactiveGridAnimation].
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
    // The cell the expansion is centred on. Set on touch-down and kept through the return animation
    // (so the shrinking cell stays on top) until the next press moves it.
    var activeRow by remember { mutableStateOf(-1) }
    var activeCol by remember { mutableStateOf(-1) }
    // Whether a finger is currently held down on the active cell: true expands and holds, false returns.
    var pressed by remember { mutableStateOf(false) }

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
                            activeRow = activeRow,
                            activeCol = activeCol,
                            pressed = pressed,
                            animation = animation,
                            onPress = {
                                activeRow = row
                                activeCol = column
                                pressed = true
                            },
                            onRelease = {
                                pressed = false
                                onItemClick(row, column)
                            },
                            onCancel = { pressed = false },
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
 * (and home scale `1`). While the grid is pressed the cell springs to its expanded target — the
 * active cell swells, neighbours translate radially outward — and holds there; on release/cancel it
 * springs back home. Because `animateTo` resumes from the cell's current value and velocity, changing
 * the target mid-flight (a quick tap, or sliding off) simply reverses the motion smoothly.
 *
 * The cell owns the press gesture: a touch-down expands the grid; lifting while still over the cell
 * commits ([onRelease], which fires the click); lifting elsewhere or sliding off the cell cancels
 * ([onCancel], which returns the grid without firing).
 */
@Composable
private fun ReactiveGridCell(
    row: Int,
    column: Int,
    activeRow: Int,
    activeCol: Int,
    pressed: Boolean,
    animation: ReactiveGridAnimation,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    val translation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scale = remember { Animatable(1f) }
    val density = LocalDensity.current
    val isActive = row == activeRow && column == activeCol

    // Callbacks are recreated each recomposition; keep the gesture's captured copies fresh.
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnCancel by rememberUpdatedState(onCancel)

    // Radial direction away from the pressed cell (euclidean) for a natural push...
    val dr = (row - activeRow).toFloat()
    val dc = (column - activeCol).toFloat()
    val distance = sqrt(dr * dr + dc * dc)
    val dirX = if (distance == 0f) 0f else dc / distance
    val dirY = if (distance == 0f) 0f else dr / distance

    // ...but concentric (chebyshev) rings for the strength tier and the stagger delay.
    val ring = max(abs(row - activeRow), abs(column - activeCol))
    val magnitude = animation.pushStrength * animation.falloff.pow((ring - 1).coerceAtLeast(0))

    val translationSpec = spring<Offset>(animation.pushDamping, animation.pushStiffness)
    val scaleSpec = spring<Float>(animation.swellDamping, animation.swellStiffness)

    // Drive the cell to its current target whenever the press state or the active cell changes.
    // Held: the active cell swells and neighbours push out (staggered by ring); otherwise, home.
    LaunchedEffect(activeRow, activeCol, pressed) {
        coroutineScope {
            launch {
                val targetScale = if (isActive && pressed) 1f + animation.pressScale else 1f
                scale.animateTo(targetScale, scaleSpec)
            }
            launch {
                if (pressed && !isActive && activeRow >= 0) {
                    delay(animation.ringDelay * ring)
                    val magnitudePx = with(density) { magnitude.toPx() }
                    translation.animateTo(Offset(dirX, dirY) * magnitudePx, translationSpec)
                } else {
                    translation.animateTo(Offset.Zero, translationSpec)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            // Keep the growing pressed cell above its retreating neighbours.
            .zIndex(if (isActive) 1f else 0f)
            .graphicsLayer {
                translationX = translation.value.x
                translationY = translation.value.y
                scaleX = scale.value
                scaleY = scale.value
            }
            .pointerInput(Unit) {
                while (true) {
                    // Detect one press inside the (restricted) pointer scope, returning whether it
                    // committed and how long it was held; the actual waiting happens outside it.
                    var committed = false
                    var heldMillis = 0L
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        currentOnPress()
                        var endTime = down.uptimeMillis
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) {
                                // Released over the cell -> commit; released after sliding off -> cancel.
                                committed = change.position.isWithin(size.width, size.height)
                                if (committed) change.consume()
                                endTime = change.uptimeMillis
                                break
                            }
                            if (!change.position.isWithin(size.width, size.height)) {
                                // Slid off while still down: cancel, return home, no click.
                                committed = false
                                endTime = change.uptimeMillis
                                break
                            }
                        }
                        heldMillis = endTime - down.uptimeMillis
                    }
                    // Hold the expansion a minimum time so even a quick tap visibly reacts before
                    // returning; `pressed` stays true (still expanding) until we release/cancel below.
                    val remaining = animation.minPressHold.inWholeMilliseconds - heldMillis
                    if (remaining > 0) delay(remaining)
                    if (committed) currentOnRelease() else currentOnCancel()
                }
            },
    ) {
        content()
    }
}

/** Whether this position falls inside a `width` × `height` box anchored at the origin. */
private fun Offset.isWithin(width: Int, height: Int): Boolean =
    x >= 0f && y >= 0f && x <= width && y <= height
