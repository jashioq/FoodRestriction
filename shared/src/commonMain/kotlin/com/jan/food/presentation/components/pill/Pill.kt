package com.jan.food.presentation.components.pill

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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

/** How long the pill takes to ease between its unselected and selected looks. */
private const val SelectionAnimationMillis = 300

/** Unselected look: a raised white chip with a [LightGray] border and black content. */
private val UnselectedBackground = Color.White
private val UnselectedBorder = LightGray
private val UnselectedContent = Color.Black
private val UnselectedElevation = 8.dp

/** Selected ("pressed in") look: a dark, flat chip with a near-black border and light content. */
private val SelectedBackground = Color(0xFF2A2A2A)
private val SelectedBorder = Color(0xFF1A1A1A)
private val SelectedContent = Color.White
private val SelectedElevation = 0.dp

/**
 * A capsule-shaped chip sharing the circle action button's look: a white fill, a [LightGray] border
 * and a soft drop shadow. When [selected] it eases into a dark, "pressed in" variant — a near-black
 * fill, dark border, light content and no drop shadow — over [SelectionAnimationMillis] ms. Any tap
 * interaction (and the press animation) is owned by the surrounding container (e.g. `ReactiveGrid`).
 *
 * The content colour is published via [LocalContentColor], so a child `Text` left at its default
 * colour automatically fades along with the pill.
 *
 * @param modifier the [Modifier] applied to the pill.
 * @param selected whether the pill is in its dark, pressed-in state.
 * @param contentPadding the inset between the pill's edge and its [content].
 * @param content the centered pill content (typically a `Text`).
 */
@Composable
fun Pill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable () -> Unit,
) {
    val spec = tween<Color>(durationMillis = SelectionAnimationMillis)
    val background by animateColorAsState(
        targetValue = if (selected) SelectedBackground else UnselectedBackground,
        animationSpec = spec,
    )
    val border by animateColorAsState(
        targetValue = if (selected) SelectedBorder else UnselectedBorder,
        animationSpec = spec,
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) SelectedContent else UnselectedContent,
        animationSpec = spec,
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) SelectedElevation else UnselectedElevation,
        animationSpec = tween(durationMillis = SelectionAnimationMillis),
    )

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(background)
            .border(width = BorderWidth, color = border, shape = CircleShape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
