package com.jan.food.presentation.screen.onboarding

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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingScreenViewModelTest {
    private val setOnboardingFinishedUseCase = mock<UseCase<Boolean, Unit>>()

    @Test
    fun `Finish delegates to setOnboardingFinishedUseCase with true`() =
        runTest {
            val capValue = Capture.slot<Boolean>()
            everySuspend { setOnboardingFinishedUseCase.call(capture(capValue, any())) } returns Result.success(Unit)

            val vmScope: CoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val vm =
                OnboardingScreenViewModel(
                    setOnboardingFinishedUseCase = setOnboardingFinishedUseCase,
                    scope = vmScope,
                    logger = TestLogger(),
                )
            advanceUntilIdle()

            vm.sendAction(OnboardingScreenAction.Finish)
            advanceUntilIdle()

            assertTrue(capValue.get())
            vmScope.cancel()
        }
}
