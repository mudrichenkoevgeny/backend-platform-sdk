package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.WebSocketInitializePayload

/**
 * Result of processing a single WebSocket [SocketFrame].
 */
public sealed interface WebSocketMessageHandlerResult {
    /**
     * Indicates that the handler chose not to process the frame.
     */
    public object NotHandled : WebSocketMessageHandlerResult

    /**
     * Marks that the frame was handled without any additional side effects.
     */
    public object Handled : WebSocketMessageHandlerResult

    /**
     * Instructs the manager to send a [socketFrame] back to the client.
     */
    public data class SendSocketFrame(
        public val socketFrame: SocketFrame
    ) : WebSocketMessageHandlerResult

    /**
     * Updates client metadata using [payload] and sends [socketFrame] to acknowledge initialization.
     */
    public data class InitializeClient(
        public val socketFrame: SocketFrame,
        public val payload: WebSocketInitializePayload
    ) : WebSocketMessageHandlerResult

    /**
     * Signals that an application-level [appError] occurred while handling the frame.
     */
    public data class Error(
        public val appError: AppError
    ) : WebSocketMessageHandlerResult
}
