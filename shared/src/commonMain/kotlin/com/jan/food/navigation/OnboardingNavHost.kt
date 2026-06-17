package com.jan.food.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jan.food.presentation.screen.onboarding.OnboardingScreen
import com.jan.food.presentation.screen.onboarding.OnboardingScreenDestination

@Composable
fun OnboardingNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = OnboardingScreenDestination,
    ) {
        composable<OnboardingScreenDestination> {
            OnboardingScreen()
        }
    }
}