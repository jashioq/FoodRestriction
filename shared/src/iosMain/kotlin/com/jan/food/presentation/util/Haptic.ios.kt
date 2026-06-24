package com.jan.food.presentation.util

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class Haptic {
    // Persistent generators kept warm: building and preparing them once (at startup, via the Koin
    // singleton) means the Taptic Engine is already spun up when the first impact fires, instead of
    // stalling the UI thread to warm it lazily. Each impact re-prepares to stay ready for the next.
    private val lightGenerator =
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val mediumGenerator =
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavyGenerator =
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)

    init {
        dispatch_async(dispatch_get_main_queue()) {
            lightGenerator.prepare()
            mediumGenerator.prepare()
            heavyGenerator.prepare()
        }
    }

    actual fun performLightImpact() = impact(lightGenerator)

    actual fun performMediumImpact() = impact(mediumGenerator)

    actual fun performHeavyImpact() = impact(heavyGenerator)

    private fun impact(generator: UIImpactFeedbackGenerator) {
        dispatch_async(dispatch_get_main_queue()) {
            generator.impactOccurred()
            generator.prepare()
        }
    }
}
