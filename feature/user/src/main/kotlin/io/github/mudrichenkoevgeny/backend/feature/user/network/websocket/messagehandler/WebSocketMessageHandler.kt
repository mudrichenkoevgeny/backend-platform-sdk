package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame

/**
 * Contract for handling a specific subset of incoming WebSocket messages.
 *
 * Implementations inspect the [SocketFrame] and either return a concrete
 * [WebSocketMessageHandlerResult] or [WebSocketMessageHandlerResult.NotHandled]
 * to let other handlers try.
 */
interface WebSocketMessageHandler {

    /**
     * Attempts to handle the incoming [frame] for the given [webSocketSessionContext].
     */
    suspend fun handle(
        frame: SocketFrame,
        webSocketSessionContext: WebSocketSessionContext
    ): WebSocketMessageHandlerResult
}