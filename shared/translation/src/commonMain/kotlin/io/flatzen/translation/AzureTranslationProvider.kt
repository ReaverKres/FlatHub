package io.flatzen.translation

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

class AzureTranslationProvider(
    private val httpClient: HttpClient,
    private val apiKey: String = TranslationApiKeys.AZURE_KEY,
    private val region: String = TranslationApiKeys.AZURE_REGION,
    private val quotaStore: TranslationQuotaStore,
) : TranslationProvider {

    override val backend: TranslationBackend = TranslationBackend.AZURE
    override val isConfigured: Boolean =
        apiKey.isNotBlank() && !apiKey.startsWith("YOUR_")

    override suspend fun remainingQuota(): QuotaStatus {
        if (!isConfigured || quotaStore.isExhausted(backend)) return QuotaStatus.Exhausted
        val used = quotaStore.usedCharacters(backend)
        return QuotaStatus.Ok(used = used, limit = AZURE_FREE_LIMIT)
    }

    override suspend fun translate(request: TranslateRequest): TranslateResult {
        if (!isConfigured) {
            throw QuotaExceededException(backend, "Azure key is not configured")
        }
        if (quotaStore.isExhausted(backend) ||
            quotaStore.usedCharacters(backend) + request.charCount() > AZURE_FREE_LIMIT
        ) {
            quotaStore.markExhausted(backend)
            throw QuotaExceededException(backend)
        }

        val target = request.targetLang.lowercase()
        val url = buildString {
            append(BASE_URL)
            append("?api-version=3.0&to=")
            append(target)
            request.sourceLang?.let { append("&from=").append(it.lowercase()) }
        }

        val translated = mutableListOf<String>()
        var detected: String? = null

        for (chunk in request.texts.chunked(MAX_TEXTS_PER_REQUEST)) {
            val chunkChars = chunk.sumOf { it.length.toLong() }
            if (quotaStore.usedCharacters(backend) + chunkChars > AZURE_FREE_LIMIT) {
                quotaStore.markExhausted(backend)
                throw QuotaExceededException(backend)
            }

            val response: HttpResponse = httpClient.post(url) {
                header("Ocp-Apim-Subscription-Key", apiKey)
                header("Ocp-Apim-Subscription-Region", region)
                contentType(ContentType.Application.Json)
                setBody(chunk.map { AzureTextBody(it) })
            }

            when (response.status) {
                HttpStatusCode.Forbidden, HttpStatusCode.TooManyRequests -> {
                    quotaStore.markExhausted(backend)
                    throw QuotaExceededException(
                        backend,
                        "Azure quota/rate limit: ${response.status}"
                    )
                }

                else -> if (!response.status.isSuccess()) {
                    throw Exception("Azure translate failed: ${response.status}")
                }
            }

            val body: List<AzureTranslateResponse> = response.body()
            translated += body.map { it.translations.firstOrNull()?.text.orEmpty() }
            if (detected == null) {
                detected = body.firstOrNull()?.detectedLanguage?.language
            }
            quotaStore.recordUsage(backend, chunkChars)
        }

        return TranslateResult(
            texts = translated,
            detectedSource = detected,
            backend = backend,
        )
    }

    companion object {
        private const val BASE_URL = "https://api.cognitive.microsofttranslator.com/translate"
        const val AZURE_FREE_LIMIT = 2_000_000L
        private const val MAX_TEXTS_PER_REQUEST = 25
    }
}

@Serializable
private data class AzureTextBody(val Text: String)

@Serializable
private data class AzureTranslateResponse(
    val detectedLanguage: AzureDetectedLanguage? = null,
    val translations: List<AzureTranslation> = emptyList(),
)

@Serializable
private data class AzureDetectedLanguage(
    val language: String,
    val score: Double = 0.0,
)

@Serializable
private data class AzureTranslation(
    val text: String,
    @SerialName("to") val toLang: String? = null,
)

internal fun TranslateRequest.charCount(): Long =
    texts.sumOf { it.length.toLong() }
