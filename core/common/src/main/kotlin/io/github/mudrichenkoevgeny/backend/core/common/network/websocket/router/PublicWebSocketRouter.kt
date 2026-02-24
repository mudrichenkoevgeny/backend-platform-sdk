package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.router

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.WebSocketContract
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicWebSocketRouter @Inject constructor(
    private val webSocketManager: WebSocketManager
) : BaseRouter {

    override fun register(route: Route) {
        route.webSocket(WebSocketContract.WS_REALTIME_PATH) {
            webSocketManager.register(this, null)
        }
    }
}