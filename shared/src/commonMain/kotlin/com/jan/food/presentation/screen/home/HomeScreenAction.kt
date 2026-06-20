package com.jan.food.presentation.screen.home

open class HomeScreenAction {
    data object Login : HomeScreenAction()
    data object Logout : HomeScreenAction()
    data class BarcodeDetected(val barcode: String?) : HomeScreenAction()
    data object CheckProduct : HomeScreenAction()
}
