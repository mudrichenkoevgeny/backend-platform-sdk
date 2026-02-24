package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.SocketFrame

interface WebSocketMessageHandler {
    suspend fun handle(frame: SocketFrame, userId: String?): WebSocketMessageHandlerResult
}