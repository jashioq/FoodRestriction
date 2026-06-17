package com.jan.food

import android.app.Application
import com.jan.food.di.KoinInitializer

class FoodApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KoinInitializer(applicationContext).init()
    }
}