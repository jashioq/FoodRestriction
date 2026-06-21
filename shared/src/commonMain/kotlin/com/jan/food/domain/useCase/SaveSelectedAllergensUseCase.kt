package com.jan.food.domain.useCase

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.repository.AllergenRepository
import com.jan.food.domain.util.UseCase

open class SaveSelectedAllergensUseCase(
    private val allergenRepository: AllergenRepository,
) : UseCase<List<Allergen>, Unit> {
    override suspend fun call(value: List<Allergen>): Result<Unit> =
        allergenRepository.saveSelectedAllergens(value)
}
