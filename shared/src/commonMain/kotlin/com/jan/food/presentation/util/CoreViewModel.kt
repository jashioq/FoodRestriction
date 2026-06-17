package com.jan.food.presentation.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jan.food.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

/**
 * A generic view model that governs state management, coroutine scope injections and logging.
 * @param S The type of the state.
 * @param A The type of the action.
 * @param initialState The initial [S] state.
 * @param scope The [CoroutineScope] to be used for coroutines (defaults to viewModelScope).
 * @param logger The [Logger] to be used for logging.
 */
abstract class CoreViewModel<S, A>(
    initialState: S,
    scope: CoroutineScope? = null,
    logger: Logger? = null,
) : ViewModel() {
    /**
     * The [CoroutineScope] for this ViewModel. Injected for testability.
     * Use [vmScope] instead of the default `viewModelScope`.
     */
    protected val vmScope = scope ?: viewModelScope

    /**
     * The [Logger] for this ViewModel. Injected for testability.
     * Use [vmLogger] instead of a new `Logger()` instance.
     */
    protected val vmLogger = logger ?: Logger()

    @Suppress("PropertyName")
    protected val stateFlow = MutableStateFlow(initialState)
    val state = stateFlow.asStateFlow()

    private val tag = this::class.simpleName ?: "CoreViewModel"
    private val actions = Channel<A>(Channel.BUFFERED)

    init {
        vmScope.launch {
            actions.consumeAsFlow()
                .collect { action ->
                    vmLogger.d(tag, "Processing action: $action")
                    action.process()
                }
        }
    }

    /**
     * Defines how a given action [A] is processed to update the state [S].
     */
    protected abstract fun A.process()

    /**
     * Sends an action to the ViewModel for processing.
     * @param action The [A] action to be sent.
     */
    fun sendAction(action: A) {
        vmLogger.d(tag, "Sending action: $action")
        actions.trySend(action)
            .onFailure { throwable ->
                vmLogger.e(tag, "Failed to send action: $action. The action channel may be full or closed.", throwable)
            }
    }

    override fun onCleared() {
        super.onCleared()
        actions.close()
    }
}