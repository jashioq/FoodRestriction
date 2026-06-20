package com.jan.food.data.dataSource.food

import kotlinx.serialization.Serializable

/**
 * Request body for the `/v1/check` endpoint.
 * @param barcode the product barcode to look up.
 * @param restrictions the dietary restrictions to evaluate against the product.
 */
@Serializable
data class CheckRequest(
    val barcode: String,
    val restrictions: List<String>,
)

/**
 * Response body for the `/v1/check` endpoint.
 * @param barcode the looked-up product barcode.
 * @param name the product name.
 * @param source the data source, one of `off`, `cache`, `unknown`.
 * @param found whether the product was found.
 * @param results the per-restriction evaluation results.
 */
@Serializable
data class CheckResponse(
    val barcode: String,
    val name: String,
    val source: String,
    val found: Boolean,
    val results: List<RestrictionResult>,
)

/**
 * The evaluation of a single dietary restriction against a product.
 * @param restriction the restriction identifier.
 * @param status the evaluation status, one of `contains`, `absent`, `unknown`.
 */
@Serializable
data class RestrictionResult(
    val restriction: String,
    val status: String,
)
