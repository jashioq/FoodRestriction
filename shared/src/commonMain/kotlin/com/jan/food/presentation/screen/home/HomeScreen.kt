package com.jan.food.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jan.food.presentation.components.button.CameraCaptureButton
import com.jan.food.presentation.components.button.MenuButton
import com.jan.food.presentation.components.button.TapPulseState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    barcode: String?,
    onMenuClick: () -> Unit,
    menuButtonPulse: TapPulseState? = null,
    viewModel: HomeScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // The camera feed is shared and hosted by the NavHost; forward its scans into the view model.
    LaunchedEffect(barcode) {
        viewModel.sendAction(HomeScreenAction.BarcodeDetected(barcode))
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
        Button(
            onClick = {
                viewModel.sendAction(
                    HomeScreenAction.Login
                )
            }
        ) {
            Text("login")
        }

        Button(
            onClick = {
                viewModel.sendAction(
                    HomeScreenAction.Logout
                )
            }
        ) {
            Text("logout")
        }

        Text(text = "id token: ${state.session?.idToken?.take(10)}\n" +
                "access token: ${state.session?.accessToken?.take(10)}\n" +
                "refresh token: ${state.session?.refreshToken?.take(10)}",
            color = Color.White,
        )

        Text(text = "barcode: ${state.productCheck?.barcode}\n" +
                "name: ${state.productCheck?.name}\n" +
                "source: ${state.productCheck?.source}\n" +
                "found: ${state.productCheck?.found}\n" +
                "results:\n${state.productCheck?.results}\n",
            color = Color.White,
        )
        }

        MenuButton(
            onClick = onMenuClick,
            // Shared with the menu's back button so the two pulse in sync across navigation.
            pulse = menuButtonPulse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp),
        )

        CameraCaptureButton(
            onClick = {
                viewModel.sendAction(HomeScreenAction.CheckProduct)
            },
            isLoading = state.isLoading,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}