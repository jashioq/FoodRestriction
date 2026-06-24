package com.jan.food.presentation.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.button.CircleActionButton
import com.jan.food.presentation.components.button.MenuButton
import com.jan.food.presentation.components.button.TapPulseState
import com.jan.food.presentation.components.camera.CameraFeedAnchor
import com.jan.food.presentation.components.camera.CameraFeedEffect
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
    CameraFeedEffect(anchor = anchor, darken = anchor == CameraFeedAnchor.REVEAL_THREE_QUARTERS)
    val offsetFraction = rememberCameraFeedOffsetFraction()
    val showingResults = state.productCheck != null && !state.isLoading

    fun guardClick(onClick: () -> Unit) {
        if (offsetFraction == 0F) onClick()
    }

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
        // touching the visible camera strip dismisses the results — on finger down, not finger up.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = slide)
                .then(
                    if (showingResults) {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                viewModel.sendAction(HomeScreenAction.DismissResults)
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Top,
//            ) {
//                Button(onClick = { viewModel.sendAction(HomeScreenAction.Login) }) {
//                    Text("login")
//                }
//
//                Button(onClick = { viewModel.sendAction(HomeScreenAction.Logout) }) {
//                    Text("logout")
//                }
//
//                Text(
//                    text = "id token: ${state.session?.idToken?.take(10)}\n" +
//                        "access token: ${state.session?.accessToken?.take(10)}\n" +
//                        "refresh token: ${state.session?.refreshToken?.take(10)}",
//                    color = Color.White,
//                )
//
//                Text(
//                    text = "barcode: ${state.productCheck?.barcode}\n" +
//                        "name: ${state.productCheck?.name}\n" +
//                        "source: ${state.productCheck?.source}\n" +
//                        "found: ${state.productCheck?.found}\n" +
//                        "results:\n${state.productCheck?.results}\n",
//                    color = Color.White,
//                )
//            }

            AnimatedVisibility(
                visible = showingResults,
                enter = fadeIn(tween(MENU_FADE_MILLIS)),
                exit = fadeOut(tween(MENU_FADE_MILLIS)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(width = 60.dp, height = 5.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.8f)),
                )
            }

            // Only available in the idle state; fades out while loading or showing results.
            AnimatedVisibility(
                visible = anchor == CameraFeedAnchor.FULL,
                enter = fadeIn(tween(MENU_FADE_MILLIS)),
                exit = fadeOut(tween(MENU_FADE_MILLIS)),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                MenuButton(
                    onClick = { guardClick { onMenuClick(state.selectedAllergens.map { it.tag }) } },
                    // Shared with the menu's back button so the two pulse in sync across navigation.
                    pulse = menuButtonPulse,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            CircleActionButton(
                onClick = { guardClick { viewModel.sendAction(HomeScreenAction.CheckProduct) } },
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

private const val MENU_FADE_MILLIS = 500
