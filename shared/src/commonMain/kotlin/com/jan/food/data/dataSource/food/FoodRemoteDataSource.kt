package com.jan.food.data.dataSource.food

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Remote data source for the app's food API.
 *
 * @param httpClient the authenticated [HttpClient] that attaches the bearer token and points at the
 * API base URL.
 */
class FoodRemoteDataSource(
    private val httpClient: HttpClient,
) {
    /**
     * Check a product against a set of dietary restrictions via `POST /v1/check`.
     * @param barcode the product barcode to look up.
     * @param restrictions the dietary restrictions to evaluate.
     */
    suspend fun checkProduct(
        barcode: String,
        restrictions: List<String>,
    ): Result<CheckResponse> = runCatching {
        httpClient.post("/v1/check") {
            contentType(ContentType.Application.Json)
            setBody(CheckRequest(barcode = barcode, restrictions = restrictions))
        }.body()
    }
}
