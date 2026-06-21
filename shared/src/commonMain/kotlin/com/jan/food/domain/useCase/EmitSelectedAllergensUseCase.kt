package com.jan.food.domain.useCase

import com.jan.food.domain.model.Allergen
import com.jan.food.domain.repository.AllergenRepository
import com.jan.food.domain.util.UseCase
import kotlinx.coroutines.flow.Flow

open class EmitSelectedAllergensUseCase(
    private val allergenRepository: AllergenRepository,
) : UseCase<Unit, Flow<List<Allergen>>> {
    override suspend fun call(value: Unit): Result<Flow<List<Allergen>>> =
        allergenRepository.emitSelectedAllergens()
}
