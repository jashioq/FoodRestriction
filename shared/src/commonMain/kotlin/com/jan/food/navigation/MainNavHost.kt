package com.jan.food.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jan.food.presentation.screen.home.HomeScreen
import com.jan.food.presentation.screen.home.HomeScreenDestination

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeScreenDestination,
    ) {
        composable<HomeScreenDestination> {
            HomeScreen()
        }
    }
}