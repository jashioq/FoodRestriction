package com.jan.food.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jan.food.presentation.screen.onboarding.OnboardingScreenAction
import com.jan.food.presentation.screen.onboarding.OnboardingScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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

        Text("id token: ${state.session?.idToken?.take(10)}\n" +
                "access token: ${state.session?.accessToken?.take(10)}\n" +
                "refresh token: ${state.session?.refreshToken?.take(10)}")

        Button(
            onClick = {
                viewModel.sendAction(
                    HomeScreenAction.CheckProduct
                )
            }
        ) {
            Text("check nutella")
        }

        Text("barcode: ${state.productCheck?.barcode}\n" +
                "name: ${state.productCheck?.name}\n" +
                "source: ${state.productCheck?.source}\n" +
                "found: ${state.productCheck?.found}\n" +
                "results: ${state.productCheck?.results}\n"
        )
    }
}