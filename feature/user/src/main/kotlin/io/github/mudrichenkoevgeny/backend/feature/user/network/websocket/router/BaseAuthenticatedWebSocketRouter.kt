package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.router

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getExpiresAt
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getJWTPrincipal
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getSessionId
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdForWebSocket
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketCloseReasons
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close

/**
 * Base router for mounting WebSocket endpoints with optional JWT authentication and scope attribution.
 *
 * Configures authentication as optional to support anonymous connections while enforcing validation
 * if credentials are provided. Delegates connection lifecycle and active framing to [WebSocketManager].
 *
 * @param webSocketManager Manager handling connection registration and frame dispatching.
 * @param path The URL path on which the WebSocket endpoint will listen.
 * @param apiScope Target audience boundary ([ApiScope.OPEN] or [ApiScope.MANAGEMENT]).
 */
abstract class BaseAuthenticatedWebSocketRouter(
    private val webSocketManager: WebSocketManager,
    private val path: String,
    private val apiScope: ApiScope
) : BaseRouter {

    override fun register(route: Route) {
        val isAuthenticationOptional = true
        val cleanPath = path.trim('/')

        route.route(cleanPath) {
            authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION, optional = isAuthenticationOptional) {
                webSocket {
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

                    val userRole = principal?.getUserRole()
                    val userSessionId = principal?.getSessionId()
                    val userSessionExpiresAt = principal?.getExpiresAt()

                    webSocketManager.register(
                        webSocketSession = this,
                        apiScope = apiScope,
                        userId = userId,
                        userRole = userRole,
                        userSessionId = userSessionId,
                        userSessionExpiresAt = userSessionExpiresAt
                    )
                }
            }
        }
    }
}