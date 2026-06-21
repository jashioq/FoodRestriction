package com.jan.food.presentation.components.button

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/**
 * Circular capture button for the camera screen: the shared [CircleIconButton] style with a
 * filled dark-gray camera icon centered inside. While [isLoading] is true the icon is replaced
 * by a spinner and the button is not clickable.
 *
 * @param onClick invoked when the button is tapped.
 * @param modifier the [Modifier] applied to the button.
 * @param isLoading when true, shows a spinner instead of the icon and disables tapping.
 */
@Composable
fun CameraCaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    CircleIconButton(
        onClick = onClick,
        modifier = modifier,
        size = 72.dp,
        isLoading = isLoading,
    ) {
        Icon(
            imageVector = CameraIcon,
            contentDescription = "Capture",
            tint = DarkGray,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** Filled "photo camera" icon, rebuilt as an [ImageVector] to avoid the material-icons dependency. */
private val CameraIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PhotoCamera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Camera body with the lens opening punched out (even-odd).
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(9f, 2f)
            lineTo(7.17f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
            verticalLineTo(18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(6f)
            curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
            horizontalLineTo(16.83f)
            lineTo(15f, 2f)
            horizontalLineTo(9f)
            close()
            // Inner lens opening (radius 5 around 12,12).
            moveTo(17f, 12f)
            curveTo(17f, 14.761f, 14.761f, 17f, 12f, 17f)
            curveTo(9.239f, 17f, 7f, 14.761f, 7f, 12f)
            curveTo(7f, 9.239f, 9.239f, 7f, 12f, 7f)
            curveTo(14.761f, 7f, 17f, 9.239f, 17f, 12f)
            close()
        }
        // Filled lens dot (radius 3.2 around 12,12).
        path(fill = SolidColor(Color.Black)) {
            moveTo(15.2f, 12f)
            curveTo(15.2f, 13.767f, 13.767f, 15.2f, 12f, 15.2f)
            curveTo(10.233f, 15.2f, 8.8f, 13.767f, 8.8f, 12f)
            curveTo(8.8f, 10.233f, 10.233f, 8.8f, 12f, 8.8f)
            curveTo(13.767f, 8.8f, 15.2f, 10.233f, 15.2f, 12f)
            close()
        }
    }.build()
}
