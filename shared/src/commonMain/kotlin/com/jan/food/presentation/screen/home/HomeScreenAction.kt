package com.jan.food.presentation.screen.home

open class HomeScreenAction {
    data object Login : HomeScreenAction()
    data object Logout : HomeScreenAction()
    data class BarcodeDetected(val barcode: String?) : HomeScreenAction()
    data object CheckProduct : HomeScreenAction()

    /** Debug-only: drives the dummy scan flow (loading for a few seconds, then dummy results). */
    data object SimulateScan : HomeScreenAction()

    /** Clears the current results, returning the screen to the idle (full-screen feed) state. */
    data object DismissResults : HomeScreenAction()
}
