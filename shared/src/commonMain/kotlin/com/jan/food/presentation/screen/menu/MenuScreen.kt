package com.jan.food.presentation.screen.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.button.BackButton
import com.jan.food.presentation.components.button.TapPulseState
import org.koin.compose.viewmodel.koinViewModel

/**
 * Full-screen menu overlay drawn on top of the shared, blurred camera feed hosted by the NavHost:
 * a translucent white scrim with a [BackButton] in the same position as the home screen's menu
 * button. Reached via a fade transition from the home screen.
 *
 * @param onBackClick invoked when the back button is tapped.
 * @param viewModel the screen's [MenuScreenViewModel].
 */
@Composable
fun MenuScreen(
    onBackClick: () -> Unit,
    backButtonPulse: TapPulseState? = null,
    viewModel: MenuScreenViewModel = koinViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.5f)),
    )

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        BackButton(
            onClick = onBackClick,
            // Shared with the home menu button so the two pulse in sync across navigation.
            pulse = backButtonPulse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp),
        )

        Text(
            text = "Menu goes here",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
