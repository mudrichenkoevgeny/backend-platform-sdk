package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles user-feature WebSocket events that do not require server-side actions.
 *
 * The messages listed in [UserWebSocketEventTypes] are considered "terminal" or
 * informational for the server; they are marked as handled to avoid propagating
 * them to generic handlers.
 */
@Singleton
class UserWebSocketMessageHandler @Inject constructor() : WebSocketMessageHandler {
    override suspend fun handle(
        frame: SocketFrame,
        webSocketSessionContext: WebSocketSessionContext
    ): WebSocketMessageHandlerResult {
        return when (frame.type) {
            UserWebSocketEventTypes.UNAUTHORIZED -> WebSocketMessageHandlerResult.Handled
            UserWebSocketEventTypes.SESSION_TERMINATED -> WebSocketMessageHandlerResult.Handled
            UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED -> WebSocketMessageHandlerResult.Handled
            UserWebSocketEventTypes.ACCOUNT_STATUS_CHANGED -> WebSocketMessageHandlerResult.Handled
            else -> WebSocketMessageHandlerResult.NotHandled
        }
    }
}