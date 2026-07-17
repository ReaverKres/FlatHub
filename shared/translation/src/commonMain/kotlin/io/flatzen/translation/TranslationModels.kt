package io.flatzen.translation

enum class TranslationBackend {
    AZURE,
    DEEPL,
}

data class TranslateRequest(
    val texts: List<String>,
    val targetLang: String,
    val sourceLang: String? = null,
)

data class TranslateResult(
    val texts: List<String>,
    val detectedSource: String?,
    val backend: TranslationBackend,
)

sealed class QuotaStatus {
    data class Ok(val used: Long, val limit: Long) : QuotaStatus() {
        val remaining: Long get() = (limit - used).coerceAtLeast(0)
        val isNearLimit: Boolean get() = remaining < limit * 0.05
    }

    data object Exhausted : QuotaStatus()
    data object Unknown : QuotaStatus()
}

class QuotaExceededException(
    val backend: TranslationBackend,
    message: String = "Quota exceeded for $backend",
) : Exception(message)

class TranslationUnavailableException(
    message: String = "All translation providers are unavailable",
) : Exception(message)

interface TranslationProvider {
    val backend: TranslationBackend

    /** False when API key is missing — provider is skipped entirely. */
    val isConfigured: Boolean
    suspend fun translate(request: TranslateRequest): TranslateResult
    suspend fun remainingQuota(): QuotaStatus
}

interface TranslationService {
    suspend fun translate(request: TranslateRequest): TranslateResult
    suspend fun refreshQuotaOrder()
}
