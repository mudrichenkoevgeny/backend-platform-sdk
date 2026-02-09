package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

data class HttpClientSettings(
    val baseUrl: String? = null,
    val requestTimeout: Long = HttpClientConfig.REQUEST_TIMEOUT,
    val connectTimeout: Long = HttpClientConfig.CONNECT_TIMEOUT,
    val socketTimeout: Long = HttpClientConfig.SOCKET_TIMEOUT,
    val maxRetries: Int = HttpClientConfig.MAX_RETRIES,
    val defaultHeaders: Map<String, String> = emptyMap()
)