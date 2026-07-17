package io.flatzen.translation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TranslationCache {
    private val mutex = Mutex()
    private val map = mutableMapOf<String, TranslateResult>()
    private val order = ArrayDeque<String>()

    suspend fun get(request: TranslateRequest): TranslateResult? = mutex.withLock {
        map[key(request)]
    }

    suspend fun put(request: TranslateRequest, result: TranslateResult) = mutex.withLock {
        val k = key(request)
        if (k !in map) {
            order.addLast(k)
            while (order.size > MAX_ENTRIES) {
                val oldest = order.removeFirst()
                map.remove(oldest)
            }
        }
        map[k] = result
    }

    private fun key(request: TranslateRequest): String =
        buildString {
            append(request.targetLang.lowercase())
            append('|')
            append(request.sourceLang.orEmpty().lowercase())
            append('|')
            request.texts.forEach { append(it.hashCode()).append(';') }
        }

    companion object {
        private const val MAX_ENTRIES = 64
    }
}
