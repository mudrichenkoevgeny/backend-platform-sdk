package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Shared WebSocket configuration values used when installing the Ktor WebSockets plugin.
 */
object WebSocketConfig {
    val PING_PERIOD: Duration = 15.seconds
    val TIMEOUT: Duration = 15.seconds
    const val MAX_FRAME_SIZE: Long = Long.MAX_VALUE
    const val MASKING: Boolean = false
}