package com.jan.food.presentation.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Stroke width of the cutout border. */
private val BorderWidth = 3.dp

/** Border color. */
private val BorderColor = Color.Blue

/** Bottom-corner rounding of the notch border (the top edge sits flush with the screen edge). */
private val NotchCornerRadius = 18.dp

/**
 * Draws a [BorderWidth] [BorderColor] outline hugging the *outside* of the device's display cutout
 * (so the whole stroke is visible, never hidden behind the cutout), or a full-width line along the
 * very top of the screen when there is no cutout. Fills the screen and is meant to be overlaid on top
 * of all other content via [CutoutBorderOverlay]; it draws nothing else and does not intercept input.
 */
@Composable
fun CutoutBorder(modifier: Modifier = Modifier) {
    val cutout = rememberDisplayCutout()
    val outlinePath = rememberCutoutOutlinePath()

    Box(modifier = modifier.fillMaxSize()) {
        // When the platform reports the exact cutout outline (Android API 31+), stroke it directly —
        // it traces the true silhouette (e.g. a perfect circle for a punch-hole) at its real position.
        // The outline is grown outward by the stroke width so the whole stroke sits outside the cutout.
        if (outlinePath != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = BorderWidth.toPx()
                drawPath(
                    path = outlinePath.grownOutward(strokePx),
                    color = BorderColor,
                    style = Stroke(width = strokePx),
                )
            }
            return@Box
        }

        if (cutout.type == DisplayCutoutType.NO_CUTOUT) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(BorderWidth)
                    .background(BorderColor),
            )
            return@Box
        }

        val shape = when (cutout.type) {
            // Punch-holes and the Dynamic Island read as fully-rounded capsules/circles.
            DisplayCutoutType.DYNAMIC_ISLAND, DisplayCutoutType.CUSTOM -> RoundedCornerShape(percent = 50)
            // Notches meet the screen's top edge squarely and round only at the bottom. Grow the
            // radius alongside the box so the rounded corners stay concentric with the cutout.
            DisplayCutoutType.LARGE_NOTCH, DisplayCutoutType.SMALL_NOTCH ->
                RoundedCornerShape(bottomStart = NotchCornerRadius + BorderWidth, bottomEnd = NotchCornerRadius + BorderWidth)
            DisplayCutoutType.NO_CUTOUT -> RoundedCornerShape(0.dp)
        }

        // Enlarge the box by the stroke width on every side and shift it out by the same amount, so
        // the inner edge of the (inward-drawn) border lands exactly on the cutout edge.
        Box(
            modifier = Modifier
                .offset(x = cutout.offset.x - BorderWidth, y = cutout.offset.y - BorderWidth)
                .size(width = cutout.size.width + BorderWidth * 2, height = cutout.size.height + BorderWidth * 2)
                .border(BorderWidth, BorderColor, shape),
        )
    }
}

/**
 * Returns a copy of this path scaled about its center so every edge moves outward by [outset]/2 (the
 * stroke half-width), leaving a centered stroke of width [outset] sitting entirely outside the
 * original outline. Cutout silhouettes are convex and roughly centered, so a center scale is a good
 * approximation of a true outward offset.
 */
private fun Path.grownOutward(outset: Float): Path {
    val bounds = getBounds()
    if (bounds.width <= 0f || bounds.height <= 0f) return this

    val scaleX = (bounds.width + outset) / bounds.width
    val scaleY = (bounds.height + outset) / bounds.height
    val matrix = Matrix().apply {
        values[Matrix.ScaleX] = scaleX
        values[Matrix.ScaleY] = scaleY
        values[Matrix.TranslateX] = bounds.center.x * (1 - scaleX)
        values[Matrix.TranslateY] = bounds.center.y * (1 - scaleY)
    }
    return Path().apply {
        addPath(this@grownOutward)
        transform(matrix)
    }
}

/**
 * Wraps [content] and overlays the [CutoutBorder] on top of it, so the cutout outline is always
 * visible above every screen. Place this around the app's navigation host.
 */
@Composable
fun CutoutBorderOverlay(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        CutoutBorder()
    }
}
