package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

import io.github.mudrichenkoevgeny.backend.core.common.logs.naming.TracingKeys
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class HttpClientProviderTest {

    private val provider = HttpClientProvider()

    @Test
    fun `create builds http client without errors`() {
        MDC.put(TracingKeys.TRACE_ID_KEY, "trace-123")

        val settings = HttpClientSettings(
            baseUrl = "https://example.com",
            requestTimeout = 1000L,
            connectTimeout = 2000L,
            socketTimeout = 3000L,
            maxRetries = 0,
            defaultHeaders = mapOf("X-Default" to "value")
        )

        val client = provider.create(settings)
        assertNotNull(client)

        MDC.remove(TracingKeys.TRACE_ID_KEY)
    }
}

