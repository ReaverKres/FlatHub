package core

import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform

sealed class NetworkResponseWrapper<out T> {
    data class Success<T>(val data: T) : NetworkResponseWrapper<T>()
    class Error(val ex: Throwable, val error: NetworkErrorInfo? = null) : NetworkResponseWrapper<Nothing>()

    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(ex: Throwable, error: NetworkErrorInfo? = null) = Error(ex, error)
    }
}

data class NetworkErrorInfo(
    val platform: FlatPlatform,
    val errorMessages: List<String>,
    /** DNS / host lookup failure (e.g. geo-blocked domains). Detected by exception type, not message text. */
    val isHostUnresolved: Boolean = false,
)

/** Walks cause chain; matches JVM [UnknownHostException] and Ktor/Kotlin [UnresolvedAddressException]. */
fun Throwable.isHostUnresolved(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        when (current::class.simpleName) {
            "UnknownHostException",
            "UnresolvedAddressException",
                -> return true
        }
        current = current.cause
    }
    return false
}

fun networkErrorInfo(
    platform: FlatPlatform,
    throwable: Throwable,
    errorMessages: List<String> = listOf(throwable.message.orEmpty()),
): NetworkErrorInfo = NetworkErrorInfo(
    platform = platform,
    errorMessages = errorMessages,
    isHostUnresolved = throwable.isHostUnresolved(),
)

val networkEmptyList: NetworkResponseWrapper.Success<List<AppFlat>> =
    NetworkResponseWrapper.success(emptyList())