package com.jan.food.presentation.util

/**
 * Expected interface for performing haptic feedback.
 *
 * Provided as an eagerly-created Koin singleton (see `platformModule`) so the platform
 * feedback engine is warmed at startup rather than on the first impact, which otherwise stalls the
 * UI thread the first time it fires. Inject it with `koinInject<Haptic>()`.
 */
expect class Haptic {
    /**
     * Performs a light haptic feedback.
     */
    fun performLightImpact()

    /**
     * Performs a medium haptic feedback.
     */
    fun performMediumImpact()

    /**
     * Performs a heavy haptic feedback.
     */
    fun performHeavyImpact()
}
