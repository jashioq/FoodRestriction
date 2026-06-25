package com.jan.food.presentation.components.cutout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jan.food.presentation.components.cutout.animations.Loading

/**
 * Renders the cutout notification's current [animation] around the device's display cutout. Fills the
 * screen and is meant to be overlaid on top of all other content via [CutoutNotification]; it draws
 * nothing else and does not intercept input.
 *
 * Each animation owns its own drawing in `cutout.animations`; this only selects which one plays.
 * [CutoutNotificationAnimation.NONE] draws nothing.
 */
@Composable
fun CutoutBorder(
    modifier: Modifier = Modifier,
    animation: CutoutNotificationAnimation,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (animation) {
            CutoutNotificationAnimation.NONE -> Unit
            CutoutNotificationAnimation.LOADING -> Loading()
        }
    }
}
