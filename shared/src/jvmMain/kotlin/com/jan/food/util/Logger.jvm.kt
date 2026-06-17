package com.jan.food.util

actual open class Logger actual constructor() {
    actual open fun e(tag: String, message: String, throwable: Throwable?) {
    }

    actual open fun d(tag: String, message: String) {
    }

    actual open fun i(tag: String, message: String) {
    }
}