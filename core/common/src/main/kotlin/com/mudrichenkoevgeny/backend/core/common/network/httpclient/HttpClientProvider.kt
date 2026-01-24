package com.mudrichenkoevgeny.backend.core.common.network.httpclient

import com.mudrichenkoevgeny.backend.core.common.constants.NetworkConstants
import com.mudrichenkoevgeny.backend.core.common.constants.TracingConstants
import com.mudrichenkoevgeny.backend.core.common.serialization.DefaultJson
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import org.slf4j.MDC

class HttpClientProvider() {
    fun create(
        config: HttpClientSettings,
        block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}
    ): HttpClient {
        return HttpClient(CIO) {
            block()

            install(ContentNegotiation) { json(DefaultJson) }

            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeout
                connectTimeoutMillis = config.connectTimeout
                socketTimeoutMillis = config.socketTimeout
            }

            install(HttpRequestRetry) {
                maxRetries = config.maxRetries
                retryIf { _, response -> !response.status.isSuccess() }
                retryOnExceptionIf { _, cause -> cause is IOException }
                exponentialDelay()
            }

            install(Logging) {
                level = LogLevel.INFO
            }

            defaultRequest {
                config.baseUrl?.let { url(it) }
                config.defaultHeaders.forEach { (key, value) ->
                    header(key, value)
                }

                val currentTraceId = MDC.get(TracingConstants.TRACE_ID_KEY)
                if (!currentTraceId.isNullOrBlank()) {
                    header(NetworkConstants.TRACE_HEADER_NAME, currentTraceId)
                }
            }
        }
    }
}