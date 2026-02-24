package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager

import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.SocketFrame
import io.ktor.server.websocket.DefaultWebSocketServerSession

interface WebSocketManager {
    suspend fun register(session: DefaultWebSocketServerSession, userId: String?)
    suspend fun sendMessageToAllUsers(frame: SocketFrame)
    suspend fun sendMessageToUser(userId: String, frame: SocketFrame)
}