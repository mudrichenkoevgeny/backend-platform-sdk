package io.github.mudrichenkoevgeny.backend.feature.user.network.websockets.messagehandler

import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.contract.SecurityWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security module WebSocket handler.
 *
 * Currently, it acknowledges security settings update frames and
 * marks them as handled. All other frames are ignored so they can be processed by other handlers.
 */
@Singleton
class SecurityWebSocketMessageHandler @Inject constructor() : WebSocketMessageHandler {
    override suspend fun handle(
        frame: SocketFrame,
        webSocketSessionContext: WebSocketSessionContext
    ): WebSocketMessageHandlerResult {
        return when (frame.type) {
            SecurityWebSocketEventTypes.OPEN_SECURITY_SETTINGS_UPDATED -> WebSocketMessageHandlerResult.Handled
            SecurityWebSocketEventTypes.MANAGEMENT_SECURITY_SETTINGS_UPDATED -> WebSocketMessageHandlerResult.Handled
            else -> WebSocketMessageHandlerResult.NotHandled
        }
    }
}
