package com.jan.food.presentation.screen.home.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jan.food.presentation.components.camera.rememberDisplayCutout

/**
 * Content shown in the revealed area above the camera feed once a scan's results are ready
 * ([CameraFeedAnchor.REVEAL_THREE_QUARTERS]). Will host the spinning alarm beam wrapped around the
 * display cutout; for now it renders the detected cutout type as a test probe.
 */
@Composable
fun HomeResultsView(modifier: Modifier = Modifier) {
    val cutoutType = rememberDisplayCutout().type
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("results — cutout: $cutoutType")
    }
}
