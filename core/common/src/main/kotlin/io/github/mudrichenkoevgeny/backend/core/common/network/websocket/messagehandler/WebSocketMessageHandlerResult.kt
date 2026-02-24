package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.SocketFrame

sealed interface WebSocketMessageHandlerResult {
    object NotHandled: WebSocketMessageHandlerResult
    data class Handled(val socketFrame: SocketFrame): WebSocketMessageHandlerResult
}