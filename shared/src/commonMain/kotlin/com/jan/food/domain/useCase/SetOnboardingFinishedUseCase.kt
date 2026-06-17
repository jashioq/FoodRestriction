package com.jan.food.domain.useCase

import com.jan.food.domain.model.ONBOARDING_FINISHED_KEY
import com.jan.food.domain.repository.DataStoreRepository
import com.jan.food.domain.util.UseCase

/**
 * Use case used for setting onboarding finished status in the datastore.
 * @param dataStoreRepository a [DataStoreRepository] instance.
 */
open class SetOnboardingFinishedUseCase(
    private val dataStoreRepository: DataStoreRepository,
) : UseCase<Boolean, Unit> {
    /**
     * @param value a [Boolean] representing onboarding finished status.
     */
    override suspend fun call(value: Boolean): Result<Unit> =
        dataStoreRepository.putBooleanPreference(
            key = ONBOARDING_FINISHED_KEY,
            value = value,
        )
}
