package com.jan.food.presentation.screen.home

import com.jan.food.domain.model.AuthSession
import com.jan.food.domain.model.ProductCheck

data class HomeScreenState(
    val session: AuthSession?,
    val productCheck: ProductCheck?,
    val isLoading: Boolean,
)
