package com.jan.food.domain.useCase

import com.jan.food.domain.model.ProductCheck
import com.jan.food.domain.repository.FoodRepository
import com.jan.food.domain.util.UseCase

/**
 * Parameters for [CheckProductUseCase].
 * @param barcode the product barcode to look up.
 * @param restrictions the dietary restrictions to evaluate.
 */
data class CheckProductParams(
    val barcode: String,
    val restrictions: List<String>,
)

/**
 * Use case used for checking a product against a set of dietary restrictions.
 * @param foodRepository a [FoodRepository] instance.
 */
open class CheckProductUseCase(
    private val foodRepository: FoodRepository,
) : UseCase<CheckProductParams, ProductCheck> {
    /**
     * @param value the [CheckProductParams] holding the barcode and restrictions.
     */
    override suspend fun call(value: CheckProductParams): Result<ProductCheck> =
        foodRepository.checkProduct(
            barcode = value.barcode,
            restrictions = value.restrictions,
        )
}
