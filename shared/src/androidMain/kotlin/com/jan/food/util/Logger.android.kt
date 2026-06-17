package com.jan.food.util

import android.util.Log

actual open class Logger {
    actual open fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    actual open fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual open fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
}