package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.config

import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WebSocketConfigTest {

    @Test
    fun `PING_PERIOD and TIMEOUT have expected values`() {
        assertEquals(15.seconds, WebSocketConfig.PING_PERIOD)
        assertEquals(15.seconds, WebSocketConfig.TIMEOUT)
    }
}

