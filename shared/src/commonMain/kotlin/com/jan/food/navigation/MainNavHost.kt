package com.jan.food.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onMenu = backStackEntry?.destination?.hasRoute(MenuScreenDestination::class) == true

    // One pulse shared by the home menu button and the menu's back button: because both read the
    // same color, tapping one carries its animation straight into the other across the cross-fade.
    val navButtonPulse = rememberTapPulseState()

    // Tap-to-focus only on the home screen; the menu uses the feed as a static blurred backdrop.
    CameraBackground(blurred = onMenu, tapToFocusEnabled = !onMenu) { barcode ->
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
