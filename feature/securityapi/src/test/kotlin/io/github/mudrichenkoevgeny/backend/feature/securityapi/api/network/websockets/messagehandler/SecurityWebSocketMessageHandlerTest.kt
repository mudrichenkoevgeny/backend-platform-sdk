package io.github.mudrichenkoevgeny.backend.feature.securityapi.api.network.websockets.messagehandler

import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.contract.SecurityWebSocketEventTypes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecurityWebSocketMessageHandlerTest {

    private companion object {
        private const val SOCKET_SESSION_ID = "s"
    }

    @Test
    fun `handle returns Handled for SECURITY_SETTINGS_UPDATED`() {
        val handler = SecurityWebSocketMessageHandler()
        val frame = mockk<SocketFrame>()
        every { frame.type } returns SecurityWebSocketEventTypes.SECURITY_SETTINGS_UPDATED

        val result = runBlocking {
            handler.handle(
                frame = frame,
                webSocketSessionContext = WebSocketSessionContext(
                    socketSessionId = SOCKET_SESSION_ID,
                    clientInfo = null,
                    userId = null,
                    userRole = null,
                    userSessionId = null
                )
            )
        }

        assertTrue(result is WebSocketMessageHandlerResult.Handled)
    }

    @Test
    fun `handle returns NotHandled for unknown type`() {
        val handler = SecurityWebSocketMessageHandler()
        val frame = mockk<SocketFrame>()
        every { frame.type } returns "unknown"

        val result = runBlocking {
            handler.handle(
                frame = frame,
                webSocketSessionContext = WebSocketSessionContext(
                    socketSessionId = SOCKET_SESSION_ID,
                    clientInfo = null,
                    userId = null,
                    userRole = null,
                    userSessionId = null
                )
            )
        }

        assertTrue(result is WebSocketMessageHandlerResult.NotHandled)
    }
}
