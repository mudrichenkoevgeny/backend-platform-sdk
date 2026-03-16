package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

/**
 * Per-client settings used by [HttpClientProvider] to configure Ktor clients.
 *
 * Allows callers to override base URL, timeouts, retry count and default headers
 * for specific integrations while still relying on [HttpClientConfig] defaults.
 */
data class HttpClientSettings(
    val baseUrl: String? = null,
    val requestTimeout: Long = HttpClientConfig.REQUEST_TIMEOUT,
    val connectTimeout: Long = HttpClientConfig.CONNECT_TIMEOUT,
    val socketTimeout: Long = HttpClientConfig.SOCKET_TIMEOUT,
    val maxRetries: Int = HttpClientConfig.MAX_RETRIES,
    val defaultHeaders: Map<String, String> = emptyMap()
)