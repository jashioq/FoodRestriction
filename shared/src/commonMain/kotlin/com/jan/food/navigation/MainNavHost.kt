package com.jan.food.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

    CameraBackground(blurred = onMenu) { barcode ->
        NavHost(
            navController = navController,
            startDestination = HomeScreenDestination,
            // Cross-fade the (transparent) screen content over the shared camera background.
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable<HomeScreenDestination> {
                HomeScreen(
                    barcode = barcode,
                    onMenuClick = { navController.navigate(MenuScreenDestination) },
                )
            }

            composable<MenuScreenDestination> {
                MenuScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }
        }
    }
}
