package com.jan.food.fixtures

import com.jan.food.util.Logger

class TestLogger : Logger() {
    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {}

    override fun d(
        tag: String,
        message: String,
    ) {}

    override fun i(
        tag: String,
        message: String,
    ) {}
}
