package io.github.mudrichenkoevgeny.backend.feature.settingsapi.network.websockets.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsWebSocketMessageHandlerTest {

    @Test
    fun `handle returns Handled for global settings updated event types`() {
        val handler = SettingsWebSocketMessageHandler()
        val eventTypes = listOf(
            SettingsWebSocketEventTypes.OPEN_GLOBAL_SETTINGS_UPDATED,
            SettingsWebSocketEventTypes.MANAGEMENT_GLOBAL_SETTINGS_UPDATED
        )

        eventTypes.forEach { type ->
            val frame = mockk<SocketFrame>()
            every { frame.type } returns type

            val result = runBlocking {
                handler.handle(
                    frame = frame,
                    webSocketSessionContext = WebSocketSessionContext(
                        socketSessionId = "s",
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
        val handler = SettingsWebSocketMessageHandler()
        val frame = mockk<SocketFrame>()
        every { frame.type } returns "unknown"

        val result = runBlocking {
            handler.handle(
                frame = frame,
                webSocketSessionContext = WebSocketSessionContext(
                    socketSessionId = "s",
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
