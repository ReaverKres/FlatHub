package io.flatzen.data.di

import de.jensklingenberg.ktorfit.Ktorfit

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

import org.koin.dsl.module

private const val HTTP_TIMEOUT: Long = 20_000
private const val KUFAR_BASE_URL: String = "https://api.kufar.by"

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
                // header("Project", platform.currentProject)
            }
        }
    }

    single<Ktorfit> {
        val client: HttpClient = get()

        Ktorfit.Builder()
            .httpClient(client)
            .baseUrl(KUFAR_BASE_URL)
            .build()
    }
}

