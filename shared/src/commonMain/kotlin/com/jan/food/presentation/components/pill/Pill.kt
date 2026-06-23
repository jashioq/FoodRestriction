package com.jan.food.presentation.components.pill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.jan.food.presentation.components.button.LightGray

/** Border thickness shared by every [Pill] — mirrors the circle action button. */
private val BorderWidth = 3.dp

/**
 * A static, capsule-shaped chip sharing the circle action button's look: a white fill, a
 * [LightGray] border and a soft drop shadow. Purely presentational — no press state or animation; any
 * interaction (and the press animation) is owned by the surrounding container (e.g. `ReactiveGrid`).
 *
 * @param modifier the [Modifier] applied to the pill.
 * @param contentPadding the inset between the pill's edge and its [content].
 * @param content the centered pill content (typically a `Text`).
 */
@Composable
fun Pill(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color.White)
            .border(width = BorderWidth, color = LightGray, shape = CircleShape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
