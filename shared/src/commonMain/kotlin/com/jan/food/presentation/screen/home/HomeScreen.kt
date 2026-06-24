package com.jan.food.presentation.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.button.CircleActionButton
import com.jan.food.presentation.components.button.MenuButton
import com.jan.food.presentation.components.button.TapPulseState
import com.jan.food.presentation.components.camera.CameraFeedAnchor
import com.jan.food.presentation.components.camera.CameraFeedAnchorEffect
import com.jan.food.presentation.components.camera.rememberCameraFeedOffsetFraction
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    barcode: String?,
    onMenuClick: (selectedAllergenTags: List<String>) -> Unit,
    menuButtonPulse: TapPulseState? = null,
    viewModel: HomeScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // The camera feed is shared and hosted by the NavHost; forward its scans into the view model.
    LaunchedEffect(barcode) {
        viewModel.sendAction(HomeScreenAction.BarcodeDetected(barcode))
    }

    // Drive the shared feed's slide position from this screen's state, and read the controller's
    // live offset so feed-attached content (buttons) moves in lock-step with the feed.
    val anchor = state.cameraFeedAnchor()
    CameraFeedAnchorEffect(anchor)
    val offsetFraction = rememberCameraFeedOffsetFraction()
    val showingResults = state.productCheck != null && !state.isLoading

    BoxWithConstraints(modifier = Modifier.fillMaxSize().displayCutoutPadding()) {
        val slide = maxHeight * offsetFraction

        // Placeholder panel in the revealed area above the feed (drawn over the white backdrop).
        Box(
            modifier = Modifier.fillMaxWidth().height(slide),
            contentAlignment = Alignment.Center,
        ) {
            when (anchor) {
                CameraFeedAnchor.REVEAL_QUARTER -> Text("loading")
                CameraFeedAnchor.REVEAL_THREE_QUARTERS -> Text("results")
                CameraFeedAnchor.FULL -> {}
            }
        }

        // Feed-attached foreground: translated in lock-step with the feed. In the results state,
        // tapping the visible camera strip dismisses the results.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = slide)
                .then(
                    if (showingResults) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { viewModel.sendAction(HomeScreenAction.DismissResults) }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Button(onClick = { viewModel.sendAction(HomeScreenAction.Login) }) {
                    Text("login")
                }

                Button(onClick = { viewModel.sendAction(HomeScreenAction.Logout) }) {
                    Text("logout")
                }

                Text(
                    text = "id token: ${state.session?.idToken?.take(10)}\n" +
                        "access token: ${state.session?.accessToken?.take(10)}\n" +
                        "refresh token: ${state.session?.refreshToken?.take(10)}",
                    color = Color.White,
                )

                Text(
                    text = "barcode: ${state.productCheck?.barcode}\n" +
                        "name: ${state.productCheck?.name}\n" +
                        "source: ${state.productCheck?.source}\n" +
                        "found: ${state.productCheck?.found}\n" +
                        "results:\n${state.productCheck?.results}\n",
                    color = Color.White,
                )
            }

            MenuButton(
                onClick = { onMenuClick(state.selectedAllergens.map { it.tag }) },
                // Shared with the menu's back button so the two pulse in sync across navigation.
                pulse = menuButtonPulse,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 16.dp),
            )

            CircleActionButton(
                onClick = { viewModel.sendAction(HomeScreenAction.CheckProduct) },
                isLoading = state.isLoading,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            ) {}


            // Debug-only trigger for the dummy scan flow: screen-fixed, doesn't slide with the feed.
            CircleActionButton(
                onClick = { viewModel.sendAction(HomeScreenAction.SimulateScan) },
                size = 48.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 32.dp),
            ) {}
        }
    }
}
