package com.jan.food

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jan.food.domain.model.OnboardingState
import com.jan.food.navigation.MainNavHost
import com.jan.food.navigation.NavigationViewModel
import com.jan.food.navigation.OnboardingNavHost
import org.jetbrains.compose.resources.painterResource

import food.shared.generated.resources.Res
import food.shared.generated.resources.compose_multiplatform
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.KoinContext

@Composable
@Preview
fun App() {
    MaterialTheme {
        GetNavHost()
    }
}

/**
 * Uses [NavigationViewModel] to determine which [NavHost] should be displayed.
 * @param navigationViewModel the [NavigationViewModel] to be used.
 */
@Composable
private fun GetNavHost(
    navigationViewModel: NavigationViewModel = koinViewModel(),
) {
    val onboardingState by navigationViewModel.state.collectAsState()

    when (onboardingState) {
        OnboardingState.LOADING -> {
            // Do nothing
        }
        OnboardingState.FINISHED -> MainNavHost()
        OnboardingState.NOT_FINISHED -> OnboardingNavHost()
    }
}
