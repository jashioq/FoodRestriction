package com.jan.food.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jan.food.presentation.components.button.rememberTapPulseState
import com.jan.food.presentation.components.camera.CameraBackground
import com.jan.food.presentation.screen.home.HomeScreen
import com.jan.food.presentation.screen.home.HomeScreenDestination
import com.jan.food.presentation.screen.menu.MenuScreen
import com.jan.food.presentation.screen.menu.MenuScreenDestination

@Composable
fun MainNavHost() {
    val navController = rememberNavController()

    val navButtonPulse = rememberTapPulseState()

    CameraBackground { barcode ->
        NavHost(
            navController = navController,
            startDestination = HomeScreenDestination,
            enterTransition = { fadeIn(tween(FADE_MILLIS)) },
            exitTransition = { fadeOut(tween(FADE_MILLIS)) },
            popEnterTransition = { fadeIn(tween(FADE_MILLIS)) },
            popExitTransition = { fadeOut(tween(FADE_MILLIS)) },
        ) {
            composable<HomeScreenDestination> {
                HomeScreen(
                    barcode = barcode,
                    onMenuClick = { tags -> navController.navigate(MenuScreenDestination(tags)) },
                    menuButtonPulse = navButtonPulse,
                )
            }

            composable<MenuScreenDestination> { entry ->
                MenuScreen(
                    initialSelectedTags = entry.toRoute<MenuScreenDestination>().selectedAllergenTags,
                    onBackClick = { navController.popBackStack(HomeScreenDestination, inclusive = false) },
                    backButtonPulse = navButtonPulse,
                )
            }
        }
    }
}

private const val FADE_MILLIS = 300
