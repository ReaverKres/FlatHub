package repository

import core.NetworkResponseWrapper
import core.networkEmptyList
import core.networkErrorInfo
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

private suspend fun FlowCollector<NetworkResponseWrapper<List<AppFlat>>>.emitPlatformError(
    platform: FlatPlatform,
    logTag: String,
    e: Throwable,
    parsedMessages: List<String>? = null,
    messageFallback: (Throwable) -> String = { it.message.orEmpty() },
) {
    println("$logTag searchFlats ex ${e.message}")
    emit(
        NetworkResponseWrapper.error(
            e,
            networkErrorInfo(
                platform = platform,
                throwable = e,
                errorMessages = parsedMessages?.takeIf { it.isNotEmpty() }
                    ?: listOf(messageFallback(e)),
            ),
        ),
    )
}

suspend fun <E : Any> parseResponseError(
    e: ResponseException,
    deserializer: KSerializer<E>,
    messages: (E) -> List<String>,
): List<String> {
    return runCatching {
        Json.decodeFromString(deserializer, e.response.bodyAsText()).let(messages)
    }.getOrElse { emptyList() }
}

suspend fun FlowCollector<NetworkResponseWrapper<List<AppFlat>>>.runFlatSearch(
    platform: FlatPlatform,
    logTag: String,
    parseHttpError: (suspend (ResponseException) -> List<String>)? = null,
    messageFallback: (Throwable) -> String = { it.message.orEmpty() },
    block: suspend FlowCollector<NetworkResponseWrapper<List<AppFlat>>>.() -> Unit,
) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        val messages = parseHttpError?.invoke(e)
        emitPlatformError(platform, logTag, e, messages, messageFallback)
    } catch (e: Exception) {
        emitPlatformError(platform, logTag, e, messageFallback = messageFallback)
    }
}

suspend fun FlowCollector<NetworkResponseWrapper<List<AppFlat>>>.emitFlats(
    flats: List<AppFlat>,
    emptyIfNoItems: Boolean = true,
) {
    if (emptyIfNoItems && flats.isEmpty()) {
        emit(networkEmptyList)
    } else {
        emit(NetworkResponseWrapper.success(flats))
    }
}

suspend fun FlowCollector<NetworkResponseWrapper<List<AppFlat>>>.emitDedupedFlats(
    flats: List<AppFlat>,
    lastEmitList: List<AppFlat>?,
    onUpdate: (List<AppFlat>) -> Unit,
) {
    if (lastEmitList == flats) {
        emit(networkEmptyList)
    } else {
        onUpdate(flats)
        emit(NetworkResponseWrapper.success(flats))
    }
}
