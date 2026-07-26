package com.jan.food.presentation.screen.home

import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.jan.food.domain.model.Allergen
import com.jan.food.domain.model.AuthSession
import com.jan.food.domain.model.ProductCheck
import com.jan.food.domain.model.RestrictionCheck
import com.jan.food.domain.useCase.CheckProductParams
import com.jan.food.domain.useCase.LoginParams
import com.jan.food.domain.util.UseCase
import com.jan.food.fixtures.TestLogger
import com.jan.food.fixtures.authSession
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
    private val loginUseCase = mock<UseCase<LoginParams, Unit>>()
    private val logoutUseCase = mock<UseCase<Unit, Unit>>()
    private val emitSessionUseCase = mock<UseCase<Unit, Flow<AuthSession?>>>()
    private val checkProductUseCase = mock<UseCase<CheckProductParams, ProductCheck>>()
    private val emitSelectedAllergensUseCase = mock<UseCase<Unit, Flow<List<Allergen>>>>()

    private val sessionFlow = MutableStateFlow<AuthSession?>(null)
    private val allergensFlow = MutableStateFlow<List<Allergen>>(emptyList())

    private fun setupEmitMocks() {
        everySuspend { emitSessionUseCase.call(Unit) } returns Result.success(sessionFlow)
        everySuspend { emitSelectedAllergensUseCase.call(Unit) } returns Result.success(allergensFlow)
    }

    private fun createViewModel(scope: CoroutineScope) =
        HomeScreenViewModel(
            loginUseCase = loginUseCase,
            logoutUseCase = logoutUseCase,
            emitSessionUseCase = emitSessionUseCase,
            checkProductUseCase = checkProductUseCase,
            emitSelectedAllergensUseCase = emitSelectedAllergensUseCase,
            scope = scope,
            logger = TestLogger(),
        )

    @Test
    fun `emitSession collector updates session in state`() =
        runTest {
            setupEmitMocks()

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                turbine.awaitItem() // initial
                advanceUntilIdle()

                val session = authSession()
                sessionFlow.emit(session)
                advanceUntilIdle()

                assertEquals(session, turbine.awaitItem().session)
                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `emitSelectedAllergens collector updates selectedAllergens in state`() =
        runTest {
            setupEmitMocks()

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                turbine.awaitItem() // initial
                advanceUntilIdle()

                allergensFlow.emit(listOf(Allergen.GLUTEN, Allergen.MILK))
                advanceUntilIdle()

                assertEquals(listOf(Allergen.GLUTEN, Allergen.MILK), turbine.awaitItem().selectedAllergens)
                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `Login action delegates to loginUseCase`() =
        runTest {
            setupEmitMocks()
            val capParams = Capture.slot<LoginParams>()
            everySuspend { loginUseCase.call(capture(capParams, any())) } returns Result.success(Unit)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            advanceUntilIdle()

            vm.sendAction(HomeScreenAction.Login)
            advanceUntilIdle()

            assertEquals("test@test.com", capParams.get().email)
            assertEquals("MyPass123!", capParams.get().password)
            vmScope.cancel()
        }

    @Test
    fun `Logout action delegates to logoutUseCase`() =
        runTest {
            setupEmitMocks()
            val capUnit = Capture.slot<Unit>()
            everySuspend { logoutUseCase.call(capture(capUnit, any())) } returns Result.success(Unit)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            advanceUntilIdle()

            vm.sendAction(HomeScreenAction.Logout)
            advanceUntilIdle()

            assertEquals(Unit, capUnit.get())
            vmScope.cancel()
        }

    @Test
    fun `CheckProduct success sets productCheck and clears loading`() =
        runTest {
            setupEmitMocks()
            val productCheck = ProductCheck("barcode123", "Test Product", "usda", true, emptyList())
            everySuspend { checkProductUseCase.call(any()) } returns Result.success(productCheck)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                turbine.awaitItem() // initial
                advanceUntilIdle()

                vm.sendAction(HomeScreenAction.BarcodeDetected("barcode123"))
                vm.sendAction(HomeScreenAction.CheckProduct)
                advanceUntilIdle()

                val loadingState = turbine.awaitItem()
                assertTrue(loadingState.isLoading)
                assertNull(loadingState.productCheck)

                val resultState = turbine.awaitItem()
                assertFalse(resultState.isLoading)
                assertEquals(productCheck, resultState.productCheck)

                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `CheckProduct failure clears loading and leaves productCheck null`() =
        runTest {
            setupEmitMocks()
            everySuspend { checkProductUseCase.call(any()) } returns
                Result.failure(RuntimeException("network error"))

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                turbine.awaitItem() // initial
                advanceUntilIdle()

                vm.sendAction(HomeScreenAction.BarcodeDetected("barcode123"))
                vm.sendAction(HomeScreenAction.CheckProduct)
                advanceUntilIdle()

                val loadingState = turbine.awaitItem()
                assertTrue(loadingState.isLoading)

                val failureState = turbine.awaitItem()
                assertFalse(failureState.isLoading)
                assertNull(failureState.productCheck)

                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `CheckProduct without prior BarcodeDetected does not update state`() =
        runTest {
            setupEmitMocks()

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            advanceUntilIdle()

            val stateBeforeAction = vm.state.value

            vm.sendAction(HomeScreenAction.CheckProduct)
            advanceUntilIdle()

            assertEquals(stateBeforeAction, vm.state.value)
            vmScope.cancel()
        }

    @Test
    fun `SimulateScan transitions through loading to dummy result`() =
        runTest {
            setupEmitMocks()

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            turbineScope {
                val turbine = vm.state.testIn(backgroundScope)

                turbine.awaitItem() // initial
                advanceUntilIdle()

                vm.sendAction(HomeScreenAction.SimulateScan)
                advanceUntilIdle()

                val loadingState = turbine.awaitItem()
                assertTrue(loadingState.isLoading)
                assertNull(loadingState.productCheck)

                val dummyResult = turbine.awaitItem()
                assertFalse(dummyResult.isLoading)
                assertEquals("0000000000000", dummyResult.productCheck?.barcode)
                assertEquals("Dummy Product", dummyResult.productCheck?.name)
                assertEquals(
                    listOf(
                        RestrictionCheck("gluten", "contains"),
                        RestrictionCheck("milk", "absent"),
                    ),
                    dummyResult.productCheck?.results,
                )

                turbine.cancel()
            }
            vmScope.cancel()
        }

    @Test
    fun `DismissResults clears productCheck and isLoading`() =
        runTest {
            setupEmitMocks()
            val productCheck = ProductCheck("barcode123", "Test Product", "usda", true, emptyList())
            everySuspend { checkProductUseCase.call(any()) } returns Result.success(productCheck)

            val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(vmScope)
            advanceUntilIdle()

            vm.sendAction(HomeScreenAction.BarcodeDetected("barcode123"))
            vm.sendAction(HomeScreenAction.CheckProduct)
            advanceUntilIdle()

            assertEquals(productCheck, vm.state.value.productCheck)

            vm.sendAction(HomeScreenAction.DismissResults)
            advanceUntilIdle()

            assertNull(vm.state.value.productCheck)
            assertFalse(vm.state.value.isLoading)
            vmScope.cancel()
        }
}
