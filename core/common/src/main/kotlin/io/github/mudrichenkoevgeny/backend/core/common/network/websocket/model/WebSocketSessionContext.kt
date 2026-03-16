package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.model

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo

/**
 * Context associated with a single WebSocket connection.
 *
 * Tracks the socket identifier, optional authenticated user/session and
 * mutable [clientInfo] that can be enriched after initialization.
 */
data class WebSocketSessionContext(
    val socketSessionId: String,
    var clientInfo: ClientInfo?,
    val userId: UserId?,
    val userSessionId: UserSessionId?
)