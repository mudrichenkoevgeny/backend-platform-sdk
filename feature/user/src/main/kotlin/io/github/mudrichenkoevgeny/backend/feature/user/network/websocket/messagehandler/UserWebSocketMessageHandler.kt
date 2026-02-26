package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserWebSocketMessageHandler @Inject constructor() : WebSocketMessageHandler {
    override suspend fun handle(
        frame: SocketFrame,
        webSocketSessionContext: WebSocketSessionContext
    ): WebSocketMessageHandlerResult {
        return when (frame.type) {
            UserWebSocketEventTypes.SESSION_TERMINATED -> WebSocketMessageHandlerResult.Handled
            UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED -> WebSocketMessageHandlerResult.Handled
            UserWebSocketEventTypes.ACCOUNT_STATUS_CHANGED -> WebSocketMessageHandlerResult.Handled
            else -> WebSocketMessageHandlerResult.NotHandled
        }
    }
}