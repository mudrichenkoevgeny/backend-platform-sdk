package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.ktor.server.websocket.DefaultWebSocketServerSession

interface WebSocketManager {
    suspend fun register(
        webSocketSession: DefaultWebSocketServerSession,
        userId: UserId?,
        userSessionId: UserSessionId?,
        userSessionExpiresAt: Long?
    )
    suspend fun sendMessageToAll(frame: SocketFrame)
    suspend fun sendMessageToUser(userId: UserId, frame: SocketFrame)
    suspend fun sendMessageToUserSession(userSessionId: UserSessionId, frame: SocketFrame)
    suspend fun sendMessageToSocket(socketId: String, frame: SocketFrame)
    suspend fun disconnectSocket(socketId: String)
}