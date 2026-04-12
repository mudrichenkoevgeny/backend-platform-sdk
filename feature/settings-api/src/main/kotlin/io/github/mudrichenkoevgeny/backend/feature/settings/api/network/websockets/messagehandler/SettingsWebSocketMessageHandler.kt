package io.github.mudrichenkoevgeny.backend.feature.settings.api.network.websockets.messagehandler

import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket frame handler for settings-related event types.
 *
 * The handler currently acknowledges events published by the settings feature and marks them as
 * handled to prevent "unknown event" processing in the common WebSocket pipeline.
 */
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