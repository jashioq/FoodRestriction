package com.jan.food.navigation

import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.jan.food.domain.model.OnboardingState
import com.jan.food.domain.util.UseCase
import com.jan.food.fixtures.TestLogger
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
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

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
    private val emitOnboardingFinishedUseCase = mock<UseCase<Unit, Flow<Boolean>>>()

    private fun createViewModel(scope: CoroutineScope) =
        NavigationViewModel(
            emitOnboardingFinishedUseCase = emitOnboardingFinishedUseCase,
            scope = scope,
            logger = TestLogger(),
        )

    @Test
    fun `initial state is LOADING`() =
        runTest {
            val flow = MutableStateFlow(false)
            everySuspend { emitOnboardingFinishedUseCase.call(Unit) } returns Result.success(flow)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                assertEquals(OnboardingState.LOADING, turbine.awaitItem())
                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `emitting true maps to FINISHED state`() =
        runTest {
            val flow = MutableStateFlow(true)
            everySuspend { emitOnboardingFinishedUseCase.call(Unit) } returns Result.success(flow)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                assertEquals(OnboardingState.LOADING, turbine.awaitItem())
                advanceUntilIdle()
                assertEquals(OnboardingState.FINISHED, turbine.awaitItem())
                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `emitting false maps to NOT_FINISHED state`() =
        runTest {
            val flow = MutableStateFlow(false)
            everySuspend { emitOnboardingFinishedUseCase.call(Unit) } returns Result.success(flow)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                assertEquals(OnboardingState.LOADING, turbine.awaitItem())
                advanceUntilIdle()
                assertEquals(OnboardingState.NOT_FINISHED, turbine.awaitItem())
                turbine.cancel()
            }
            vmScope.cancel()
        }
}
