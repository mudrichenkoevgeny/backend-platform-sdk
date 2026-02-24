package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdFromPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.WebSocketContract
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticatedWebSocketRouter @Inject constructor(
    private val webSocketManager: WebSocketManager
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION, optional = true) {
            webSocket(WebSocketContract.WS_REALTIME_PATH) {
                val userId = (call.getUserIdFromPayload() as? AppResult.Success)?.data

                webSocketManager.register(this, userId?.asHexDashString())
            }
        }
    }
}