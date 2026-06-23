package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CameraPreview(
    modifier: Modifier,
    onBarcodeScanned: (String?) -> Unit,
    blurred: Boolean,
    tapToFocusEnabled: Boolean,
) {
    TODO("JVM/Desktop is hot-reload only; camera preview is not wired for this target.")
}
