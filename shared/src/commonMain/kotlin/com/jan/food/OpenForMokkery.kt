package com.jan.food

/**
 * Marks an otherwise-final class as `open` (via the Kotlin all-open plugin) so Mokkery can mock it
 * in tests. Mokkery's compiler plugin cannot mock final classes; annotate in-module data sources
 * that a test needs to mock rather than exposing an interface purely for testing.
 */
@Target(AnnotationTarget.CLASS)
annotation class OpenForMokkery
