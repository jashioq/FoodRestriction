package com.jan.food.presentation.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LightGray = Color(0xFFD9D9D9)
internal val DarkGray = Color(0xFF3F3F3F)

/** Border thickness shared by every [CircleIconButton]. */
private val BorderWidth = 3.dp

/**
 * Reusable circular button: light-gray fill, white border and a soft drop shadow on all sides.
 * Callers supply the [size] and the centered [content] (typically an [androidx.compose.material3.Icon]).
 * While [isLoading] is true the [content] is replaced by a spinner and the button is not clickable.
 *
 * @param onClick invoked when the button is tapped.
 * @param modifier the [Modifier] applied to the button.
 * @param size the diameter of the button.
 * @param isLoading when true, shows a spinner instead of [content] and disables tapping.
 * @param content the centered button content.
 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    isLoading: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(LightGray)
            .border(width = BorderWidth, color = Color.White, shape = CircleShape)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 10.dp,
                // Inset by the border on each side, then a hair more so the ring sits inside.
                modifier = Modifier.size(size - BorderWidth * 3),
            )
        } else {
            content()
        }
    }
}
