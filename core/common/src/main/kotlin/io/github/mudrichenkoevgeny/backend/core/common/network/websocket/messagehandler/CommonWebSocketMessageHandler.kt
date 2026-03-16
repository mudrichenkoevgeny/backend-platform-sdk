package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonApiFields
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.payload.WebSocketInitializePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Default [WebSocketMessageHandler] implementation for common control frames
 * such as ping–pong and client initialization.
 */
@Singleton
class CommonWebSocketMessageHandler @Inject constructor() : WebSocketMessageHandler {

    override suspend fun handle(
        frame: SocketFrame,
        webSocketSessionContext: WebSocketSessionContext
    ): WebSocketMessageHandlerResult {
        return when (frame.type) {
            CommonWebSocketEventTypes.PING -> handlePing()
            CommonWebSocketEventTypes.PONG -> WebSocketMessageHandlerResult.Handled
            CommonWebSocketEventTypes.INITIALIZE -> handleInitialize(frame)
            CommonWebSocketEventTypes.INITIALIZED_SUCCESS -> WebSocketMessageHandlerResult.Handled
            else -> WebSocketMessageHandlerResult.NotHandled
        }
    }

    private fun handlePing(): WebSocketMessageHandlerResult {
        return WebSocketMessageHandlerResult.SendSocketFrame(
            SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = CommonWebSocketEventTypes.PONG,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun handleInitialize(frame: SocketFrame): WebSocketMessageHandlerResult {
        val payloadElement = frame.payload ?: return WebSocketMessageHandlerResult.Error(
            CommonError.MissingRequiredField(CommonApiFields.PAYLOAD)
        )

        val payload = try {
            FoundationJson.decodeFromJsonElement<WebSocketInitializePayload>(payloadElement)
        } catch (e: Exception) {
            return WebSocketMessageHandlerResult.Error(
                CommonError.InvalidJsonBody(e.message)
            )
        }

        return WebSocketMessageHandlerResult.InitializeClient(
            socketFrame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = CommonWebSocketEventTypes.INITIALIZED_SUCCESS,
                timestamp = System.currentTimeMillis()
            ),
            payload = payload
        )
    }
}