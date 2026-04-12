package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.router

import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.WebSocketContract
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public WebSocket entry point for real-time updates.
 *
 * Registers an unauthenticated WebSocket endpoint at [WebSocketContract.WS_REALTIME_PATH]
 * and delegates session handling to [WebSocketManager].
 */
@Singleton
class PublicWebSocketRouter @Inject constructor(
    private val webSocketManager: WebSocketManager
) : BaseRouter {

    override fun register(route: Route) {
        route.webSocket(WebSocketContract.WS_REALTIME_PATH) {
            webSocketManager.register(
                webSocketSession = this,
                userId = null,
                userRole = null,
                userSessionId = null,
                userSessionExpiresAt = null
            )
        }
    }
}