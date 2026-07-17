package io.flatzen.translation

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DeepLTranslationProvider(
    private val httpClient: HttpClient,
    private val apiKey: String = TranslationApiKeys.DEEPL_KEY,
    private val quotaStore: TranslationQuotaStore,
) : TranslationProvider {

    override val backend: TranslationBackend = TranslationBackend.DEEPL
    override val isConfigured: Boolean =
        apiKey.isNotBlank() && !apiKey.startsWith("YOUR_")

    override suspend fun remainingQuota(): QuotaStatus {
        if (!isConfigured || quotaStore.isExhausted(backend)) return QuotaStatus.Exhausted
        return try {
            val usage: DeepLUsageResponse = httpClient.get("$BASE_URL/usage") {
                header("Authorization", "DeepL-Auth-Key $apiKey")
            }.body()
            if (usage.characterCount >= usage.characterLimit) {
                quotaStore.markExhausted(backend)
                QuotaStatus.Exhausted
            } else {
                QuotaStatus.Ok(used = usage.characterCount, limit = usage.characterLimit)
            }
        } catch (_: Throwable) {
            QuotaStatus.Unknown
        }
    }

    override suspend fun translate(request: TranslateRequest): TranslateResult {
        if (!isConfigured) {
            throw QuotaExceededException(backend, "DeepL key is not configured")
        }
        when (val quota = remainingQuota()) {
            QuotaStatus.Exhausted -> throw QuotaExceededException(backend)
            is QuotaStatus.Ok -> {
                if (quota.remaining < request.charCount()) {
                    quotaStore.markExhausted(backend)
                    throw QuotaExceededException(backend)
                }
            }

            QuotaStatus.Unknown -> Unit
        }

        val response: HttpResponse = httpClient.post("$BASE_URL/translate") {
            header("Authorization", "DeepL-Auth-Key $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                DeepLTranslateRequest(
                    text = request.texts,
                    targetLang = request.targetLang.uppercase(),
                    sourceLang = request.sourceLang?.uppercase(),
                )
            )
        }

        when (response.status) {
            HttpStatusCode.Forbidden, HttpStatusCode.PaymentRequired,
            HttpStatusCode.TooManyRequests -> {
                quotaStore.markExhausted(backend)
                throw QuotaExceededException(backend, "DeepL quota/rate limit: ${response.status}")
            }

            else -> if (!response.status.isSuccess()) {
                throw Exception("DeepL translate failed: ${response.status}")
            }
        }

        val body: DeepLTranslateResponse = response.body()
        val translated = body.translations.map { it.text }
        val detected = body.translations.firstOrNull()?.detectedSourceLanguage

        quotaStore.recordUsage(backend, request.charCount())
        return TranslateResult(
            texts = translated,
            detectedSource = detected,
            backend = backend,
        )
    }

    companion object {
        private const val BASE_URL = "https://api-free.deepl.com/v2"
    }
}

@Serializable
private data class DeepLTranslateRequest(
    val text: List<String>,
    @SerialName("target_lang") val targetLang: String,
    @SerialName("source_lang") val sourceLang: String? = null,
)

@Serializable
private data class DeepLTranslateResponse(
    val translations: List<DeepLTranslation> = emptyList(),
)

@Serializable
private data class DeepLTranslation(
    val text: String,
    @SerialName("detected_source_language") val detectedSourceLanguage: String? = null,
)

@Serializable
private data class DeepLUsageResponse(
    @SerialName("character_count") val characterCount: Long,
    @SerialName("character_limit") val characterLimit: Long,
)
