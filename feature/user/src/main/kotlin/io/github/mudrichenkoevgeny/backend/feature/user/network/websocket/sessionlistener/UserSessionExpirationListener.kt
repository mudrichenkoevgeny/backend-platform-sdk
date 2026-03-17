package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model.WebSocketSessionContext
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.sessionlistener.WebSocketSessionListener
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Schedules WebSocket disconnect when the authenticated user session expires.
 *
 * When a session is registered with a non-null expiration timestamp, this listener:
 * - waits until shortly before the expiration time (buffered),
 * - sends a [UserWebSocketEventTypes.UNAUTHORIZED] frame to the socket,
 * - waits a short grace period to let the client react,
 * - disconnects the socket via [WebSocketManager].
 *
 * The scheduled job is cancelled when the socket closes to avoid leaking coroutines.
 */
@Singleton
class UserSessionExpirationListener @Inject constructor(
    @param:BackgroundScope private val scope: CoroutineScope
) : WebSocketSessionListener {

    private val expirationJobs = ConcurrentHashMap<String, Job>()

    override fun onSessionRegistered(
        webSocketManager: WebSocketManager,
        session: DefaultWebSocketServerSession,
        context: WebSocketSessionContext,
        expiresAt: Long?
    ) {
        if (expiresAt == null) return

        val socketId = context.socketSessionId
        val currentTime = System.currentTimeMillis()
        val delayTime = (expiresAt - currentTime) - TOKEN_EXPIRATION_BUFFER_MS

        expirationJobs[socketId] = scope.launch {
            if (delayTime > 0) delay(delayTime)

            webSocketManager.sendMessageToSocket(
                socketId,
                SocketFrame(
                    id = Uuid.random().toHexDashString(),
                    type = UserWebSocketEventTypes.UNAUTHORIZED,
                    timestamp = System.currentTimeMillis()
                )
            )

            delay(NOTIFY_BEFORE_CLOSE_DELAY_MS)

            webSocketManager.disconnectSocket(socketId)
        }
    }

    override fun onSessionClosed(context: WebSocketSessionContext) {
        expirationJobs.remove(context.socketSessionId)?.cancel()
    }

    companion object {
        private const val TOKEN_EXPIRATION_BUFFER_MS = 5000L
        private const val NOTIFY_BEFORE_CLOSE_DELAY_MS = 2000L
    }
}