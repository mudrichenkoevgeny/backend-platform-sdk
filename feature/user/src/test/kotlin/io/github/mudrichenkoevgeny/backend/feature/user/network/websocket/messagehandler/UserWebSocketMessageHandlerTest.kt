package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserWebSocketMessageHandlerTest {

    private val handler = UserWebSocketMessageHandler()

    @Test
    fun `handle marks known user events as handled`() {
        val knownTypes = listOf(
            UserWebSocketEventTypes.UNAUTHORIZED,
            UserWebSocketEventTypes.SESSION_TERMINATED,
            UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED,
            UserWebSocketEventTypes.ACCOUNT_STATUS_CHANGED
        )

        knownTypes.forEach { type ->
            val result = runBlocking {
                handler.handle(
                    frame = SocketFrame(id = FRAME_ID, type = type, timestamp = FRAME_TIMESTAMP),
                    webSocketSessionContext = context()
                )
            }

            assertEquals(WebSocketMessageHandlerResult.Handled, result)
        }
    }

    @Test
    fun `handle returns not handled for unknown event type`() {
        val result = runBlocking {
            handler.handle(
                frame = SocketFrame(id = FRAME_ID, type = UNKNOWN_TYPE, timestamp = FRAME_TIMESTAMP),
                webSocketSessionContext = context()
            )
        }

        assertEquals(WebSocketMessageHandlerResult.NotHandled, result)
    }

    private fun context(): WebSocketSessionContext {
        return WebSocketSessionContext(
            socketSessionId = SOCKET_ID,
            clientInfo = null,
            userId = null,
            userSessionId = null
        )
    }

    private companion object {
        const val FRAME_ID = "frame-id"
        const val FRAME_TIMESTAMP = 123L
        const val SOCKET_ID = "socket-id"

        const val UNKNOWN_TYPE = "unknown"
    }
}

