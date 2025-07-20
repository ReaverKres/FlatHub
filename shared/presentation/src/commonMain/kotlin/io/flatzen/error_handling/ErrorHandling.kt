package io.flatzen.error_handling

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface LCE<T> {
    data class Loading<T>(private val stub: Unit = Unit) : LCE<T>
    data class Error<T>(val message: String?, val throwable: Throwable) : LCE<T>
    data class Content<T>(val value: T) : LCE<T>
}

@Suppress("UNCHECKED_CAST")
fun <I, O> LCE<I>.process(
    onLoading: () -> O = { Unit as O },
    onError: (message: String?, error: Throwable) -> O = { _, _ -> Unit as O },
    onSuccess: (I) -> O = { Unit as O }
): O {
    return when(this) {
        is LCE.Content<I> -> onSuccess(value)
        is LCE.Error<*> -> onError(message, throwable)
        else -> onLoading()
    }
}

fun <I, O> Flow<I>.asLCE(contentMapper:(input: I) -> O): Flow<LCE<O>> {
    return map { LCE.Content(contentMapper(it)) as LCE<O> }
        .onStart { emit(LCE.Loading()) }
        .catch {
            if (it is Exception) {
//                errorHandler.handle(it)
            }
            emit(LCE.Error(it.message, it))
        }
}

fun <I> Flow<I>.asLCE(): Flow<LCE<I>> {
    return map { LCE.Content(it) as LCE<I> }
        .onStart { emit(LCE.Loading()) }
        .catch {
            if (it is Exception) {
//                errorHandler.handle(it)
            }
            emit(LCE.Error(it.message, it))
        }
}