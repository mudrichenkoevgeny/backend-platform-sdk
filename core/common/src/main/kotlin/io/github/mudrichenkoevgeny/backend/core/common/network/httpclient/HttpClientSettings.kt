package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

data class HttpClientSettings(
    val baseUrl: String? = null,
    val requestTimeout: Long = HttpClientConstants.REQUEST_TIMEOUT,
    val connectTimeout: Long = HttpClientConstants.CONNECT_TIMEOUT,
    val socketTimeout: Long = HttpClientConstants.SOCKET_TIMEOUT,
    val maxRetries: Int = HttpClientConstants.MAX_RETRIES,
    val defaultHeaders: Map<String, String> = emptyMap()
)