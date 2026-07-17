package io.flatzen.translation

class FailoverTranslationService(
    providers: List<TranslationProvider>,
    private val quotaStore: TranslationQuotaStore,
    private val cache: TranslationCache,
) : TranslationService {

    private val providers: List<TranslationProvider> =
        providers.filter { it.isConfigured }

    override suspend fun refreshQuotaOrder() {
        if (providers.isEmpty()) return
        val ordered = quotaStore.activeOrder()
            .mapNotNull { backend -> providers.find { it.backend == backend } }
        for (provider in ordered) {
            when (val status = provider.remainingQuota()) {
                QuotaStatus.Exhausted -> quotaStore.markExhausted(provider.backend)
                is QuotaStatus.Ok -> {
                    if (status.remaining > 0) {
                        quotaStore.putPreferredFirst(provider.backend)
                        return
                    }
                    quotaStore.markExhausted(provider.backend)
                }

                QuotaStatus.Unknown -> Unit
            }
        }
    }

    override suspend fun translate(request: TranslateRequest): TranslateResult {
        if (request.texts.isEmpty() || request.texts.all { it.isBlank() }) {
            return TranslateResult(
                texts = request.texts,
                detectedSource = null,
                backend = providers.firstOrNull()?.backend ?: TranslationBackend.DEEPL,
            )
        }

        if (providers.isEmpty()) {
            throw TranslationUnavailableException("No translation providers configured")
        }

        cache.get(request)?.let { return it }

        refreshQuotaOrder()

        val orderedProviders = quotaStore.activeOrder()
            .mapNotNull { backend -> providers.find { it.backend == backend } }
            .ifEmpty { providers }

        var lastError: Throwable? = null
        for (provider in orderedProviders) {
            when (val quota = provider.remainingQuota()) {
                QuotaStatus.Exhausted -> continue
                is QuotaStatus.Ok -> if (quota.remaining <= 0) {
                    quotaStore.markExhausted(provider.backend)
                    continue
                }

                QuotaStatus.Unknown -> Unit
            }
            try {
                val result = provider.translate(request)
                cache.put(request, result)
                return result
            } catch (e: QuotaExceededException) {
                quotaStore.markExhausted(e.backend)
                lastError = e
            } catch (e: Throwable) {
                lastError = e
            }
        }

        throw lastError ?: TranslationUnavailableException()
    }
}
