package com.jan.food.domain.model

/**
 * The result of checking a product against a set of dietary restrictions.
 * @param barcode the looked-up product barcode.
 * @param name the product name.
 * @param source the data source, one of `off`, `cache`, `unknown`.
 * @param found whether the product was found.
 * @param results the per-restriction evaluation results.
 */
data class ProductCheck(
    val barcode: String,
    val name: String,
    val source: String,
    val found: Boolean,
    val results: List<RestrictionCheck>,
)

/**
 * The evaluation of a single dietary restriction against a product.
 * @param restriction the restriction identifier.
 * @param status the evaluation status, one of `contains`, `absent`, `unknown`.
 */
data class RestrictionCheck(
    val restriction: String,
    val status: String,
)
