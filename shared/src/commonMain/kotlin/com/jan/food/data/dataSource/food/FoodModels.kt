package com.jan.food.data.dataSource.food

import kotlinx.serialization.Serializable

/**
 * Request body for the `/v1/check` endpoint.
 * @param requestedRestrictions the dietary restrictions to evaluate against the product.
 * @param barcode the product barcode to look up.
 */
@Serializable
data class CheckRequest(
    val barcode: String,
    val requestedRestrictions: List<String>,
)

/**
 * Response body for the `/v1/check` endpoint.
 * @param subject the looked-up product.
 * @param source the data source, one of `usda`, `off`, `cache`, `unknown`.
 * @param found whether the product was found.
 * @param results the per-restriction evaluation results.
 */
@Serializable
data class CheckResponse(
    val subject: CheckSubject,
    val source: String,
    val found: Boolean,
    val results: List<RestrictionResult>,
)

/**
 * The product a `/v1/check` response refers to.
 * @param type the subject type, e.g. `product`.
 * @param barcode the looked-up product barcode.
 * @param name the product name, `null` when the product was not found.
 */
@Serializable
data class CheckSubject(
    val type: String,
    val barcode: String,
    val name: String? = null,
)

/**
 * The evaluation of a single dietary restriction against a product.
 * @param restriction the restriction identifier.
 * @param status the evaluation status, one of `contains`, `may_contain`, `absent`, `unknown`.
 */
@Serializable
data class RestrictionResult(
    val restriction: String,
    val status: String,
)
