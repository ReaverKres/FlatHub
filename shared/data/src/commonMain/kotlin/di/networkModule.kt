package di

import core.KtorConverterFactory
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val HTTP_TIMEOUT: Long = 20_000
private const val KUFAR_BASE_URL: String = "https://api.kufar.by/"
private const val ONLINER_BASE_URL: String = "https://r.onliner.by/"
private const val REALT_BASE_URL: String = "https://realt.by/"
private const val DOMOVITA_BASE_URL: String = "https://api.domovita.by/"
private const val FLATHUB_BASE_URL: String = "https://flatzen-back.onrender.com/"

val networkModule = module {

    single<Json>(named("defaultJson")) {
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        }
    }

    single<HttpClient> {
        HttpClient {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = HTTP_TIMEOUT
            }

            install(ContentNegotiation) {
                json(get<Json>(named("defaultJson")))
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client: $message")
                    }
                }
                level = LogLevel.ALL
            }

            install(DefaultRequest) {
                contentType(ContentType.Application.Json)
            }
        }
    }
    single<HttpClient>(qualifier = DataQualifiers.HTML_KTOR_CLIENT) {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = HTTP_TIMEOUT
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTML Client: $message")
                    }
                }
                level = LogLevel.HEADERS
            }
        }
    }

    single<Ktorfit>(qualifier = DataQualifiers.KUFAR_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(KUFAR_BASE_URL)
            .converterFactories(KtorConverterFactory())
            .build()
    }

    single<Ktorfit>(qualifier = DataQualifiers.ONLINER_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(ONLINER_BASE_URL)
            .converterFactories(KtorConverterFactory())
            .build()
    }

    single<Ktorfit>(qualifier = DataQualifiers.REALT_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(REALT_BASE_URL)
            .converterFactories(KtorConverterFactory())
            .build()
    }

    single<Ktorfit>(qualifier = DataQualifiers.DOMOVITA_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(DOMOVITA_BASE_URL)
            .converterFactories(KtorConverterFactory())
            .build()
    }

    single<Ktorfit>(qualifier = DataQualifiers.SUBSCRIPTIONS_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(FLATHUB_BASE_URL)
            .converterFactories(KtorConverterFactory())
            .build()
    }
}

