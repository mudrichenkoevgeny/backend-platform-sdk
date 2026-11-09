package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Shared WebSocket configuration values used when installing the Ktor WebSockets plugin.
 */
public object WebSocketConfig {
    public val PING_PERIOD: Duration = 15.seconds
    public val TIMEOUT: Duration = 15.seconds
    public const val MAX_FRAME_SIZE: Long = Long.MAX_VALUE
    public const val MASKING: Boolean = false
}
