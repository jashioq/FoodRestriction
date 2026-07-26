package com.jan.food.presentation.screen.menu

import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.jan.food.domain.model.Allergen
import com.jan.food.domain.util.UseCase
import com.jan.food.fixtures.TestLogger
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.matcher.capture.get
import dev.mokkery.mock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MenuScreenViewModelTest {
    private val emitSelectedAllergensUseCase = mock<UseCase<Unit, Flow<List<Allergen>>>>()
    private val saveSelectedAllergensUseCase = mock<UseCase<List<Allergen>, Unit>>()

    private fun createViewModel(
        scope: CoroutineScope,
        initialSelected: Set<Allergen> = emptySet(),
    ) = MenuScreenViewModel(
        initialSelected = initialSelected,
        emitSelectedAllergensUseCase = emitSelectedAllergensUseCase,
        saveSelectedAllergensUseCase = saveSelectedAllergensUseCase,
        scope = scope,
        logger = TestLogger(),
    )

    @Test
    fun `emitSelectedAllergens collector mirrors selection into state`() =
        runTest {
            val allergensFlow = MutableStateFlow<List<Allergen>>(emptyList())
            everySuspend { emitSelectedAllergensUseCase.call(Unit) } returns Result.success(allergensFlow)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(scope = vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                turbine.awaitItem() // initial: empty set
                advanceUntilIdle() // start collector

                allergensFlow.emit(listOf(Allergen.GLUTEN, Allergen.MILK))
                advanceUntilIdle()

                assertEquals(setOf(Allergen.GLUTEN, Allergen.MILK), turbine.awaitItem().selectedAllergens)
                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `ToggleAllergen adds allergen when absent`() =
        runTest {
            val allergensFlow = MutableStateFlow<List<Allergen>>(emptyList())
            everySuspend { emitSelectedAllergensUseCase.call(Unit) } returns Result.success(allergensFlow)
            val capAllergens = Capture.slot<List<Allergen>>()
            everySuspend { saveSelectedAllergensUseCase.call(capture(capAllergens, any())) } returns Result.success(Unit)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(scope = vmScope)
            advanceUntilIdle()

            vm.sendAction(MenuScreenAction.ToggleAllergen(Allergen.GLUTEN))
            advanceUntilIdle()

            assertEquals(listOf(Allergen.GLUTEN), capAllergens.get())
            vmScope.cancel()
        }

    @Test
    fun `ToggleAllergen removes allergen when present`() =
        runTest {
            val allergensFlow = MutableStateFlow(listOf(Allergen.GLUTEN))
            everySuspend { emitSelectedAllergensUseCase.call(Unit) } returns Result.success(allergensFlow)
            val capAllergens = Capture.slot<List<Allergen>>()
            everySuspend { saveSelectedAllergensUseCase.call(capture(capAllergens, any())) } returns Result.success(Unit)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(scope = vmScope, initialSelected = setOf(Allergen.GLUTEN))
            advanceUntilIdle()

            vm.sendAction(MenuScreenAction.ToggleAllergen(Allergen.GLUTEN))
            advanceUntilIdle()

            assertTrue(capAllergens.get().isEmpty())
            vmScope.cancel()
        }
}
