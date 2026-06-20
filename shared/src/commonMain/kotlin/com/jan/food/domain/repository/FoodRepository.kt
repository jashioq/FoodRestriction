package com.jan.food.domain.repository

import com.jan.food.domain.model.ProductCheck

interface FoodRepository {
    /**
     * Check a product against a set of dietary restrictions.
     * @param barcode the product barcode to look up.
     * @param restrictions the dietary restrictions to evaluate.
     */
    suspend fun checkProduct(
        barcode: String,
        restrictions: List<String>,
    ): Result<ProductCheck>
}
