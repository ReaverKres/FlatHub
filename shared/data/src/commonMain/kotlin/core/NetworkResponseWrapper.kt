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
    val errorMessages: List<String>
)

val networkEmptyList: NetworkResponseWrapper.Success<List<AppFlat>> =
    NetworkResponseWrapper.success(emptyList())