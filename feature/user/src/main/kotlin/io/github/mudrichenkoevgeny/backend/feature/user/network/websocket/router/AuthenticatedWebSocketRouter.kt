package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.router

import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getExpiresAt
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getJWTPrincipal
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdForWebSocket
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.WebSocketContract
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketCloseReasons
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticatedWebSocketRouter @Inject constructor(
    private val webSocketManager: WebSocketManager
) : BaseRouter {

    override fun register(route: Route) {
        val isAuthenticationOptional = true

        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION, optional = isAuthenticationOptional) {
            webSocket(WebSocketContract.WS_REALTIME_PATH) {
                val principal = call.getJWTPrincipal()

                val userIdResult = principal?.getUserIdForWebSocket(isOptional = isAuthenticationOptional)
                    ?: if (isAuthenticationOptional) {
                        AppResult.Success(null)
                    } else {
                        AppResult.Error(UserError.InvalidAccessToken())
                    }

                val userId = when (userIdResult) {
                    is AppResult.Success -> userIdResult.data
                    is AppResult.Error -> {
                        close(
                            CloseReason(
                                code = CloseReason.Codes.CANNOT_ACCEPT,
                                message = UserWebSocketCloseReasons.AUTH_FAILED
                            )
                        )
                        return@webSocket
                    }
                }

                val userSessionId = principal?.getUserSessionId()
                val userSessionExpiresAt = principal?.getExpiresAt()

                webSocketManager.register(
                    webSocketSession = this,
                    userId = userId,
                    userSessionId = userSessionId,
                    userSessionExpiresAt = userSessionExpiresAt
                )
            }
        }
    }
}