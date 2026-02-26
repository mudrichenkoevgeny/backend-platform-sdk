package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.payload.WebSocketInitializePayload

sealed interface WebSocketMessageHandlerResult {
    object NotHandled : WebSocketMessageHandlerResult
    object Handled : WebSocketMessageHandlerResult
    data class SendSocketFrame(val socketFrame: SocketFrame) : WebSocketMessageHandlerResult
    data class InitializeClient(
        val socketFrame: SocketFrame,
        val payload: WebSocketInitializePayload
    ) : WebSocketMessageHandlerResult
    data class Error(val appError: AppError) : WebSocketMessageHandlerResult
}