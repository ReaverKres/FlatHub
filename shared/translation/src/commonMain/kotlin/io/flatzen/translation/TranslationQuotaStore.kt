package io.flatzen.translation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Tracks per-backend usage for the current calendar month.
 * Azure has no client usage API on F0 — we count locally.
 * Exhausted flags reset when the month changes.
 */
class TranslationQuotaStore(
    initialOrder: List<TranslationBackend> = defaultOrder(),
) {
    private val mutex = Mutex()
    private var monthKey: String = currentMonthKey()
    private val used = mutableMapOf(
        TranslationBackend.AZURE to 0L,
        TranslationBackend.DEEPL to 0L,
    )
    private val exhausted = mutableSetOf<TranslationBackend>()
    private val defaultPreferredOrder: List<TranslationBackend> =
        initialOrder.ifEmpty { defaultOrder() }
    private var preferredOrder: List<TranslationBackend> = defaultPreferredOrder

    suspend fun usedCharacters(backend: TranslationBackend): Long = mutex.withLock {
        rollMonthIfNeeded()
        used[backend] ?: 0L
    }

    suspend fun isExhausted(backend: TranslationBackend): Boolean = mutex.withLock {
        rollMonthIfNeeded()
        backend in exhausted
    }

    suspend fun markExhausted(backend: TranslationBackend) = mutex.withLock {
        rollMonthIfNeeded()
        exhausted += backend
        preferredOrder = preferredOrder.filterNot { it == backend } + backend
    }

    suspend fun recordUsage(backend: TranslationBackend, chars: Long) = mutex.withLock {
        rollMonthIfNeeded()
        used[backend] = (used[backend] ?: 0L) + chars
    }

    suspend fun activeOrder(): List<TranslationBackend> = mutex.withLock {
        rollMonthIfNeeded()
        preferredOrder
    }

    suspend fun putPreferredFirst(backend: TranslationBackend) = mutex.withLock {
        rollMonthIfNeeded()
        preferredOrder = listOf(backend) + preferredOrder.filterNot { it == backend }
    }

    private fun rollMonthIfNeeded() {
        val now = currentMonthKey()
        if (now != monthKey) {
            monthKey = now
            used.keys.forEach { used[it] = 0L }
            exhausted.clear()
            preferredOrder = defaultPreferredOrder
        }
    }

    private fun currentMonthKey(): String {
        val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${date.year}-${date.month.number.toString().padStart(2, '0')}"
    }

    companion object {
        /** Azure first when configured; otherwise DeepL. */
        fun defaultOrder(): List<TranslationBackend> = buildList {
            if (TranslationApiKeys.isAzureConfigured) add(TranslationBackend.AZURE)
            if (TranslationApiKeys.isDeepLConfigured) add(TranslationBackend.DEEPL)
            if (isEmpty()) {
                add(TranslationBackend.AZURE)
                add(TranslationBackend.DEEPL)
            }
        }
    }
}
