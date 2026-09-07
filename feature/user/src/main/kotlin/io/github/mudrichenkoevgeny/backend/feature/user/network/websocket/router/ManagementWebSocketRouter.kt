package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.router

import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.WebSocketContract
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management WebSocket entry point for real-time updates.
 *
 * Registers an optionally authenticated WebSocket endpoint at [WebSocketContract.WS_MANAGEMENT_REALTIME_PATH]
 * assigned to [ApiScope.MANAGEMENT] and delegates session handling to [WebSocketManager].
 */
@Singleton
class ManagementWebSocketRouter @Inject constructor(
    webSocketManager: WebSocketManager
) : BaseAuthenticatedWebSocketRouter(
    webSocketManager = webSocketManager,
    path = WebSocketContract.WS_MANAGEMENT_REALTIME_PATH,
    apiScope = ApiScope.MANAGEMENT
)