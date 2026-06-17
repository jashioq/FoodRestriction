package com.jan.food.util

expect open class Logger() {
    open fun e(tag: String, message: String, throwable: Throwable?)
    open fun d(tag: String, message: String)
    open fun i(tag: String, message: String)
}