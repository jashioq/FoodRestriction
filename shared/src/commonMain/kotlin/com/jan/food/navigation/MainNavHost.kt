package com.jan.food.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jan.food.presentation.screen.home.HomeScreen
import com.jan.food.presentation.screen.home.HomeScreenDestination
import com.jan.food.presentation.screen.menu.MenuScreen
import com.jan.food.presentation.screen.menu.MenuScreenDestination

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeScreenDestination,
        // Cross-fade between every destination.
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
    ) {
        composable<HomeScreenDestination> {
            HomeScreen(
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
