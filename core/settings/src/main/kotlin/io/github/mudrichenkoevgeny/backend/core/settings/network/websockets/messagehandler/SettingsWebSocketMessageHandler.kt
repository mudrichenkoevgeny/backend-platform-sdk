package io.github.mudrichenkoevgeny.backend.core.settings.network.websockets.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsWebSocketMessageHandler @Inject constructor() : WebSocketMessageHandler {
    override suspend fun handle(
        frame: SocketFrame,
        webSocketSessionContext: WebSocketSessionContext
    ): WebSocketMessageHandlerResult {
        return when (frame.type) {
            SettingsWebSocketEventTypes.GLOBAL_SETTINGS_UPDATED -> WebSocketMessageHandlerResult.Handled
            else -> WebSocketMessageHandlerResult.NotHandled
        }
    }
}