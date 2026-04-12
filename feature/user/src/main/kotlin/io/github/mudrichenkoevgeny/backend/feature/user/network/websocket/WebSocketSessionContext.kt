package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Context associated with a single WebSocket connection.
 *
 * Tracks the socket identifier, optional authenticated user/session and
 * mutable [clientInfo] that can be enriched after initialization.
 */
data class WebSocketSessionContext(
    val socketSessionId: String,
    val userId: UserId?,
    val userRole: UserRole?,
    val userSessionId: UserSessionId?,
    var clientInfo: ClientInfo?
)