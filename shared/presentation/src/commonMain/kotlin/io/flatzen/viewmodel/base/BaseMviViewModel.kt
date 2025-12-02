package io.flatzen.viewmodel.base

import androidx.lifecycle.ViewModel
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

abstract class BaseMviViewModel<A : MviAction, S : MviState,EV: MviEvent, EF : MviEffect>(
    private val dispatchers: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {

    val viewModelScope = CoroutineScope(SupervisorJob() + dispatchers)

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<EF>()
    val effect: SharedFlow<EF> = _effect.asSharedFlow()

    /** Обязательно переопределить начальное состояние */
    abstract fun initialState(): S

    /** Обработка action -> Flow<Effect> */
    abstract suspend fun handleIntent(action: A, currentState: S): Flow<EV>

    /** Сводим Event в новое State */
    abstract suspend fun reduce(event: EV, currentState: S): S

    protected open suspend fun onEvent(event: EV): EF? {
        return null
    }

    fun onIntent(intent: A) {
        flowOf(intent)
            .flatMapConcat { handleIntent(it, state.value) }
            .onEach { action -> onEvent(action)?.let { _effect.emit(it) } }
            .map { reduce(it, state.value) }
            .onEach {
                _state.value = it
                //TODO: Вставить проверку на дебаг сборку
                println("STATE_LOG ${"$this $it"}")

            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}