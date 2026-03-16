package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HttpClientSettingsTest {

    @Test
    fun `defaults are taken from HttpClientConfig`() {
        val settings = HttpClientSettings()

        assertEquals(HttpClientConfig.REQUEST_TIMEOUT, settings.requestTimeout)
        assertEquals(HttpClientConfig.CONNECT_TIMEOUT, settings.connectTimeout)
        assertEquals(HttpClientConfig.SOCKET_TIMEOUT, settings.socketTimeout)
        assertEquals(HttpClientConfig.MAX_RETRIES, settings.maxRetries)
        assertEquals(emptyMap<String, String>(), settings.defaultHeaders)
    }
}

