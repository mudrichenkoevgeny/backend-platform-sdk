package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.payload.WebSocketInitializePayload

/**
 * Result of processing a single WebSocket [SocketFrame].
 */
sealed interface WebSocketMessageHandlerResult {

    /**
     * Indicates that the handler chose not to process the frame.
     */
    object NotHandled : WebSocketMessageHandlerResult

    /**
     * Marks that the frame was handled without any additional side effects.
     */
    object Handled : WebSocketMessageHandlerResult

    /**
     * Instructs the manager to send a [socketFrame] back to the client.
     */
    data class SendSocketFrame(val socketFrame: SocketFrame) : WebSocketMessageHandlerResult

    /**
     * Updates client metadata using [payload] and sends [socketFrame] to acknowledge initialization.
     */
    data class InitializeClient(
        val socketFrame: SocketFrame,
        val payload: WebSocketInitializePayload
    ) : WebSocketMessageHandlerResult

    /**
     * Signals that an application-level [appError] occurred while handling the frame.
     */
    data class Error(val appError: AppError) : WebSocketMessageHandlerResult
}