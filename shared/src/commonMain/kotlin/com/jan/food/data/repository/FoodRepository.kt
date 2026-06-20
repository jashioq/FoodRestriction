package com.jan.food.data.repository

import com.jan.food.data.dataSource.food.CheckResponse
import com.jan.food.data.dataSource.food.FoodRemoteDataSource
import com.jan.food.domain.model.ProductCheck
import com.jan.food.domain.model.RestrictionCheck

class FoodRepository(
    private val foodRemoteDataSource: FoodRemoteDataSource,
) : com.jan.food.domain.repository.FoodRepository {

    override suspend fun checkProduct(
        barcode: String,
        restrictions: List<String>,
    ): Result<ProductCheck> = runCatching {
        foodRemoteDataSource.checkProduct(barcode, restrictions)
            .getOrThrow()
            .toDomain()
    }

    private fun CheckResponse.toDomain(): ProductCheck =
        ProductCheck(
            barcode = barcode,
            name = name,
            source = source,
            found = found,
            results = results.map { RestrictionCheck(restriction = it.restriction, status = it.status) },
        )
}
