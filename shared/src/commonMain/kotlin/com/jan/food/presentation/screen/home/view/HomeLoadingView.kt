package com.jan.food.presentation.screen.home.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Content shown in the revealed strip above the camera feed while a scan is being checked
 * ([CameraFeedAnchor.REVEAL_QUARTER]).
 */
@Composable
fun HomeLoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("loading")
    }
}
