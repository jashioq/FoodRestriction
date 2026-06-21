package com.jan.food.domain.useCase

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.repository.AllergenRepository
import com.jan.food.domain.util.UseCase

open class GetAvailableAllergensUseCase(
    private val allergenRepository: AllergenRepository,
) : UseCase<Unit, List<Allergen>> {
    override suspend fun call(value: Unit): Result<List<Allergen>> =
        allergenRepository.getAvailableAllergens()
}
