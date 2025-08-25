package di

import de.jensklingenberg.ktorfit.Ktorfit

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.ContentEncoding
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

val networkModule = module {

    single<Json> {
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        }
    }

    single<HttpClient> {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = HTTP_TIMEOUT
            }

            install(ContentNegotiation) {
                json(get())
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
//                // Критически важные заголовки для API Kufar
//                header("Referer", "https://re.kufar.by/")
//                header("Origin", "https://re.kufar.by")
//                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
//
//                // Дополнительные заголовки для более естественного вида
//                header("Accept", "*/*")
//                header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
//                header("Sec-Fetch-Dest", "empty")
//                header("Sec-Fetch-Mode", "cors")
//                header("Sec-Fetch-Site", "same-site")
//
//                // Возможно понадобятся эти заголовки
//                header("x-segmentation", "routing=web_re;platform=web;application=ad_view")

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
            .build()
    }

    single<Ktorfit>(qualifier = DataQualifiers.ONLINER_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(ONLINER_BASE_URL)
            .build()
    }

    single<Ktorfit>(qualifier = DataQualifiers.REALT_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(REALT_BASE_URL)
            .build()
    }

    single<Ktorfit>(qualifier = DataQualifiers.DOMOVITA_KTORFIT) {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(DOMOVITA_BASE_URL)
            .build()
    }
}

