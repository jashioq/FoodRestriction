package com.jan.food.presentation.components.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Circular back button: the shared [BlurCircleButton] frosted-darkening style with a white
 * back ("chevron-left") icon centered inside. Mirrors [MenuButton] so the two can occupy the
 * same on-screen position across screens.
 *
 * @param onClick invoked when the button is tapped.
 * @param modifier the [Modifier] applied to the button.
 */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BlurCircleButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = BackIcon,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Back ("chevron-left") icon, built as an [ImageVector] to avoid the material-icons dependency. */
private val BackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Back",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(15f, 5f)
            lineTo(8f, 12f)
            lineTo(15f, 19f)
        }
    }.build()
}
