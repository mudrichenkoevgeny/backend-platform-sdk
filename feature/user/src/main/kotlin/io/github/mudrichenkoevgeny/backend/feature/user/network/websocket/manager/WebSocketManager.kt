package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager

import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.ktor.server.websocket.DefaultWebSocketServerSession

/**
 * Abstraction over WebSocket connections and messaging.
 *
 * Implementations keep track of active sessions and allow sending frames
 * to all clients, a specific user, a specific user session, or a single socket.
 */
interface WebSocketManager {

    /**
     * Registers a new WebSocket [webSocketSession] and associates it with optional
     * [userId] and [userSessionId] context.
     *
     * @param userSessionExpiresAt optional expiration timestamp for the associated session.
     */
    suspend fun register(
        webSocketSession: DefaultWebSocketServerSession,
        userId: UserId?,
        userRole: UserRole?,
        userSessionId: UserSessionId?,
        userSessionExpiresAt: Long?
    )

    suspend fun sendMessageToAll(frame: SocketFrame)
    suspend fun sendMessageToUser(userId: UserId, frame: SocketFrame)
    suspend fun sendMessageToUserSession(userSessionId: UserSessionId, frame: SocketFrame)
    suspend fun sendMessageToSocket(socketId: String, frame: SocketFrame)
    suspend fun disconnectSocket(socketId: String)
}