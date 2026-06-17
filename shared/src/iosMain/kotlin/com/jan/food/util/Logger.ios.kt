package com.jan.food.util

import platform.Foundation.NSLog

actual open class Logger {
    actual open fun e(tag: String, message: String, throwable: Throwable?) {

        if (throwable != null) {
            NSLog("ERROR: [$tag] $message. Throwable: $throwable CAUSE ${throwable.cause}")
        } else {
            NSLog("ERROR: [$tag] $message")
        }
    }

    actual open fun d(tag: String, message: String) {
        NSLog("DEBUG: [$tag] $message")
    }

    actual open fun i(tag: String, message: String) {
        NSLog("INFO: [$tag] $message")
    }
}