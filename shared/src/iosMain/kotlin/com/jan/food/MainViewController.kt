package com.jan.food

import androidx.compose.ui.window.ComposeUIViewController
import com.jan.food.di.KoinInitializer

fun MainViewController() = ComposeUIViewController(
    configure = { KoinInitializer().init() }
) { App() }