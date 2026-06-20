package com.jan.food.presentation.screen.home

import com.jan.food.domain.model.AuthSession
import com.jan.food.domain.model.ProductCheck
import com.jan.food.domain.useCase.CheckProductParams
import com.jan.food.domain.useCase.LoginParams
import com.jan.food.domain.util.UseCase
import com.jan.food.presentation.util.CoreViewModel
import com.jan.food.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TBA
 */
class HomeScreenViewModel(
    private val loginUseCase: UseCase<LoginParams, Unit>,
    private val logoutUseCase: UseCase<Unit, Unit>,
    private val emitSessionUseCase: UseCase<Unit, Flow<AuthSession?>>,
    private val checkProductUseCase: UseCase<CheckProductParams, ProductCheck>,
    scope: CoroutineScope? = null,
    logger: Logger? = null,
) : CoreViewModel<HomeScreenState, HomeScreenAction>(
    initialState = HomeScreenState(
        session = null,
        productCheck = null,
    ),
    scope = scope,
    logger = logger,
) {
    /** Most recently scanned barcode, or null when nothing is in view. Updated only from [process]. */
    private var latestBarcode: String? = null

    init {
        vmScope.launch {
            emitSessionUseCase.call(Unit)
                .onSuccess { sessions ->
                    sessions.collect { session ->
                        stateFlow.update { state ->
                            state.copy(
                                session = session,
                            )
                        }
                        vmLogger.d("session tokens", session.toString())
                    }
                }
        }
    }

    override fun HomeScreenAction.process() {
        when (this@process) {
            is HomeScreenAction.Login -> {
                vmScope.launch {
                    loginUseCase.call(
                        LoginParams(
                            email = DEFAULT_EMAIL,
                            password = DEFAULT_PASSWORD,
                        ),
                    )
                }
            }

            is HomeScreenAction.Logout -> {
                vmScope.launch {
                    logoutUseCase.call(Unit)
                }
            }

            is HomeScreenAction.BarcodeDetected -> {
                latestBarcode = barcode
            }

            is HomeScreenAction.CheckProduct -> {
                val barcode = latestBarcode
                if (barcode == null) {
                    vmLogger.d("check skipped", "no barcode in view")
                    return
                }
                vmScope.launch {
                    checkProductUseCase.call(
                        CheckProductParams(
                            barcode = barcode,
                            restrictions = DEFAULT_RESTRICTIONS,
                        ),
                    ).onSuccess { productCheck ->
                        stateFlow.update { state ->
                            state.copy(
                                productCheck = productCheck,
                            )
                        }
                    }.onFailure {
                        vmLogger.e("check error", it.message?: "", null)
                    }
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_EMAIL = "test@test.com"
        const val DEFAULT_PASSWORD = "MyPass123!"
        val DEFAULT_RESTRICTIONS = listOf("peanut", "tree_nut", "milk", "halal")
    }
}
