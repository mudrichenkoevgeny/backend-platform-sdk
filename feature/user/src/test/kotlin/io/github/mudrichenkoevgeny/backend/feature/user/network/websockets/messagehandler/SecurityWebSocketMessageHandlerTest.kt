package io.github.mudrichenkoevgeny.backend.feature.user.network.websockets.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.contract.SecurityWebSocketEventTypes
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecurityWebSocketMessageHandlerTest {

    private companion object {
        private const val SOCKET_SESSION_ID = "s"
    }

    @Test
    fun `handle returns Handled for security settings updated event types`() {
        val handler = SecurityWebSocketMessageHandler()
        val eventTypes = listOf(
            SecurityWebSocketEventTypes.OPEN_SECURITY_SETTINGS_UPDATED,
            SecurityWebSocketEventTypes.MANAGEMENT_SECURITY_SETTINGS_UPDATED
        )

        eventTypes.forEach { type ->
            val frame = SocketFrame(type = type, payload = null, metadata = emptyMap(), timestamp = 0L)

            val result = runBlocking {
                handler.handle(
                    frame = frame,
                    webSocketSessionContext = WebSocketSessionContext(
                        socketSessionId = SOCKET_SESSION_ID,
                        apiScope = ApiScope.OPEN,
                        clientInfo = null,
                        userId = null,
                        userRole = null,
                        userSessionId = null
                    )
                )
            }

            assertTrue(result is WebSocketMessageHandlerResult.Handled)
        }
    }

    @Test
    fun `handle returns NotHandled for unknown type`() {
        val handler = SecurityWebSocketMessageHandler()
        val frame = SocketFrame(type = "unknown", payload = null, metadata = emptyMap(), timestamp = 0L)

        val result = runBlocking {
            handler.handle(
                frame = frame,
                webSocketSessionContext = WebSocketSessionContext(
                    socketSessionId = SOCKET_SESSION_ID,
                    apiScope = ApiScope.OPEN,
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
