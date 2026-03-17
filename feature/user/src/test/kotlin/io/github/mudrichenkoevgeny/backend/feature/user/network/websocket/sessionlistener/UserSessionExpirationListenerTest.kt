package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserSessionExpirationListenerTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private val listener = UserSessionExpirationListener(scope = scope)

    @Test
    fun `onSessionRegistered sends unauthorized then disconnects after expiration`() = scope.runTest {
        val webSocketManager: WebSocketManager = mockk(relaxed = true)
        val session: DefaultWebSocketServerSession = mockk(relaxed = true)

        val context = context(socketId = SOCKET_ID)
        val expiresAt = System.currentTimeMillis() + EXPIRES_IN_MS

        listener.onSessionRegistered(
            webSocketManager = webSocketManager,
            session = session,
            context = context,
            expiresAt = expiresAt
        )

        // Not yet expired (buffered) -> no message.
        dispatcher.scheduler.advanceTimeBy(UNAUTHORIZED_DELAY_MS - BEFORE_UNAUTHORIZED_MARGIN_MS)
        dispatcher.scheduler.runCurrent()
        coVerify(exactly = 0) { webSocketManager.sendMessageToSocket(any(), any()) }
        coVerify(exactly = 0) { webSocketManager.disconnectSocket(any()) }

        // Expires -> notify unauthorized.
        dispatcher.scheduler.advanceTimeBy(BEFORE_UNAUTHORIZED_MARGIN_MS)
        dispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) {
            webSocketManager.sendMessageToSocket(
                SOCKET_ID,
                withArg { frame ->
                    assertEquals(UserWebSocketEventTypes.UNAUTHORIZED, frame.type)
                }
            )
        }

        // Grace period before disconnect.
        dispatcher.scheduler.advanceTimeBy(DISCONNECT_AFTER_NOTIFY_MS)
        dispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) { webSocketManager.disconnectSocket(SOCKET_ID) }
    }

    @Test
    fun `onSessionClosed cancels scheduled expiration actions`() = scope.runTest {
        val webSocketManager: WebSocketManager = mockk(relaxed = true)
        val session: DefaultWebSocketServerSession = mockk(relaxed = true)

        val context = context(socketId = SOCKET_ID)
        val expiresAt = System.currentTimeMillis() + EXPIRES_IN_MS

        listener.onSessionRegistered(
            webSocketManager = webSocketManager,
            session = session,
            context = context,
            expiresAt = expiresAt
        )

        listener.onSessionClosed(context)

        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { webSocketManager.sendMessageToSocket(any(), any()) }
        coVerify(exactly = 0) { webSocketManager.disconnectSocket(any()) }
    }

    private fun context(socketId: String): WebSocketSessionContext {
        return WebSocketSessionContext(
            socketSessionId = socketId,
            clientInfo = null,
            userId = null,
            userSessionId = null
        )
    }

    private companion object {
        const val SOCKET_ID = "socket-id"

        // Listener buffers expiration by 5 seconds and waits 2 seconds after notifying.
        const val TOKEN_EXPIRATION_BUFFER_MS = 5000L
        const val UNAUTHORIZED_DELAY_MS = 1000L
        const val EXPIRES_IN_MS = TOKEN_EXPIRATION_BUFFER_MS + UNAUTHORIZED_DELAY_MS
        const val BEFORE_UNAUTHORIZED_MARGIN_MS = 100L

        const val DISCONNECT_AFTER_NOTIFY_MS = 2000L
    }
}

