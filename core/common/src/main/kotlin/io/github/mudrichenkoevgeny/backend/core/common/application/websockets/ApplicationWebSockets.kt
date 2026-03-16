package io.github.mudrichenkoevgeny.backend.core.common.application.websockets

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.config.WebSocketConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout

/**
 * Installs and configures Ktor WebSockets using shared [WebSocketConfig] defaults.
 *
 * Sets ping period, session timeout, maximum frame size and masking strategy
 * so that all services have consistent WebSocket behavior.
 */
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = WebSocketConfig.PING_PERIOD
        timeout = WebSocketConfig.TIMEOUT
        maxFrameSize = WebSocketConfig.MAX_FRAME_SIZE
        masking = WebSocketConfig.MASKING
    }
}