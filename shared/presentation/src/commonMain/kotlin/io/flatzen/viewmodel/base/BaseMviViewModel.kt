package io.flatzen.viewmodel.base

import androidx.lifecycle.ViewModel
import io.flatzen.mvi.MviState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent

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

    /** Сводим Effect в новое State */
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
//                Logger.(CoreConstants.STATE_LOG) {
//                    "$this $it"
//                }

            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}