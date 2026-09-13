package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

fun createTestHttpClientSettings(
    baseUrl: String = "https://example.com",
    requestTimeout: Long = 1000L,
    connectTimeout: Long = 2000L,
    socketTimeout: Long = 3000L,
    maxRetries: Int = 0,
    defaultHeaders: Map<String, String> = mapOf("X-Default" to "value")
) = HttpClientSettings(
    baseUrl = baseUrl,
    requestTimeout = requestTimeout,
    connectTimeout = connectTimeout,
    socketTimeout = socketTimeout,
    maxRetries = maxRetries,
    defaultHeaders = defaultHeaders
)
