package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

import io.github.mudrichenkoevgeny.backend.core.common.logs.naming.TracingKeys
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A factory for creating and configuring pre-configured Ktor [HttpClient] instances.
 * * **Usage**: This provider is designed for outbound communication with external
 * services, third-party APIs, or other microservices.
 *
 * This provider centralizes common concerns such as:
 * - **Serialization**: Pre-configured with the project-wide `FoundationJson`.
 * - **Resilience**: Built-in retry mechanisms and timeouts to prevent cascading failures.
 * - **Observability**: Integrated logging and distributed tracing via MDC.
 */
@Singleton
class HttpClientProvider @Inject constructor() {
    /**
     * Creates a new [HttpClient] instance with the provided settings for external integration.
     *
     * @param config The [HttpClientSettings] containing baseUrl, timeouts, retry policies, and default headers.
     * @param block An optional configuration block for additional fine-tuning of the client
     * (e.g., adding specific plugins or interceptors for a particular external service).
     * @return A fully initialized [HttpClient] ready for outbound requests.
     */
    fun create(
        config: HttpClientSettings,
        block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}
    ): HttpClient {
        return HttpClient(CIO) {
            block()

            install(ContentNegotiation) { json(FoundationJson) }

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

                val currentTraceId = MDC.get(TracingKeys.TRACE_ID_KEY)
                if (!currentTraceId.isNullOrBlank()) {
                    header(CommonHttpHeaders.TRACE_HEADER_NAME, currentTraceId)
                }
            }
        }
    }
}