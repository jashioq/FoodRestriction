package com.jan.food.domain.model

/**
 * An allergen the backend can evaluate a product against.
 *
 * @property tag the identifier sent to / received from the backend (e.g. `"tree_nut"`).
 */
enum class Allergen(val tag: String) {
    MILK("milk"),
    EGG("egg"),
    PEANUT("peanut"),
    TREE_NUT("tree_nut"),
    SOY("soy"),
    GLUTEN("gluten"),
    SESAME("sesame"),
    FISH("fish"),
    CRUSTACEAN("crustacean"),
    MOLLUSC("mollusc"),
    CELERY("celery"),
    MUSTARD("mustard"),
    LUPIN("lupin"),
    SULPHITES("sulphites"),
    ;

    companion object {
        /**
         * The [Allergen] whose [tag] equals [tag], or `null` if none matches.
         * @param tag the backend allergen tag to resolve.
         */
        fun fromTag(tag: String): Allergen? = entries.firstOrNull { it.tag == tag }
    }
}
