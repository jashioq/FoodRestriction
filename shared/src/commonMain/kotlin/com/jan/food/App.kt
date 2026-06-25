package com.jan.food

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.jan.food.domain.model.OnboardingState
import com.jan.food.navigation.MainNavHost
import com.jan.food.navigation.NavigationViewModel
import com.jan.food.navigation.OnboardingNavHost
import org.koin.compose.viewmodel.koinViewModel

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
