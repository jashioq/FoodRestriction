package com.jan.food.presentation.components.cutout.animations

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.cutout.DisplayCutout
import com.jan.food.presentation.components.cutout.DisplayCutoutType

/** Bottom-corner rounding of the notch outline (the top edge sits flush with the screen edge). */
private val NotchCornerRadius = 18.dp

/**
 * Builds the centerline [Path] that runs directly along the device's display-cutout edge, so a centered
 * stroke straddles the edge (correcting for minor geometry measurement errors). Uses the platform's
 * exact outline when available ([outlinePath]); otherwise derives the outline from the per-type shape
 * and cutout box, or an open top-edge line when there's no cutout.
 */
internal fun buildCutoutOutlinePath(
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

/** Whether the cutout outline is a closed loop (vs. the open top-edge line used when there's no cutout). */
internal fun DisplayCutout.hasClosedOutline(outlinePath: Path?): Boolean =
    outlinePath != null || type != DisplayCutoutType.NO_CUTOUT
