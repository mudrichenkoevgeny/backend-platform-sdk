package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener

import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.ktor.server.websocket.DefaultWebSocketServerSession

/**
 * Hook for reacting to WebSocket lifecycle events.
 */
interface WebSocketSessionListener {

    /**
     * Called when a new WebSocket [session] is registered with the given [context].
     *
     * @param expiresAt optional timestamp when the associated session should expire.
     */
    fun onSessionRegistered(
        webSocketManager: WebSocketManager,
        session: DefaultWebSocketServerSession,
        context: WebSocketSessionContext,
        expiresAt: Long?
    )

    /**
     * Called after a WebSocket session has been closed and cleaned up.
     */
    fun onSessionClosed(
        context: WebSocketSessionContext
    )
}