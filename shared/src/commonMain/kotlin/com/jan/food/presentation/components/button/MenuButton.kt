package com.jan.food.presentation.components.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Circular menu button: the shared [BlurCircleButton] frosted-darkening style with a white
 * hamburger icon centered inside.
 *
 * @param onClick invoked when the button is tapped.
 * @param modifier the [Modifier] applied to the button.
 * @param pulse an optional shared color animation (see [BlurCircleButton]); `null` lets the button
 * animate itself.
 */
@Composable
fun MenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pulse: TapPulseState? = null,
) {
    BlurCircleButton(
        onClick = onClick,
        modifier = modifier,
        pulse = pulse,
    ) {
        Icon(
            imageVector = MenuIcon,
            contentDescription = "Menu",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Hamburger ("menu") icon, built as an [ImageVector] to avoid the material-icons dependency. */
private val MenuIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Menu",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Three evenly spaced horizontal bars.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(4f, 7f)
            horizontalLineTo(20f)
            moveTo(4f, 12f)
            horizontalLineTo(20f)
            moveTo(4f, 17f)
            horizontalLineTo(20f)
        }
    }.build()
}
