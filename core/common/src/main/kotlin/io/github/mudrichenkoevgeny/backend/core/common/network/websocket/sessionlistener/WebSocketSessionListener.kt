package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.sessionlistener

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model.WebSocketSessionContext
import io.ktor.server.websocket.DefaultWebSocketServerSession

interface WebSocketSessionListener {
    fun onSessionRegistered(
        webSocketManager: WebSocketManager,
        session: DefaultWebSocketServerSession,
        context: WebSocketSessionContext,
        expiresAt: Long?
    )

    fun onSessionClosed(
        context: WebSocketSessionContext
    )
}