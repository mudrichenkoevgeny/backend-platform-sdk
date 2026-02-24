package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.SocketFrame
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Singleton
class CommonWebSocketMessageHandler @Inject constructor() : WebSocketMessageHandler {

    override suspend fun handle(frame: SocketFrame, userId: String?): WebSocketMessageHandlerResult {
        return when (frame.type) {
            CommonWebSocketEventTypes.PING -> handlePing()
            else -> WebSocketMessageHandlerResult.NotHandled
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun handlePing(): WebSocketMessageHandlerResult {
        return WebSocketMessageHandlerResult.Handled(
            SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = CommonWebSocketEventTypes.PONG,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}