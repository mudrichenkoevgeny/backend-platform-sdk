package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket

import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Context associated with an active WebSocket connection.
 *
 * Encapsulates connection lifecycle metadata, audience routing scope, optional authentication
 * details, and client device characteristics.
 *
 * @property socketSessionId Unique internal identifier assigned to this specific socket connection.
 * @property apiScope Target audience and routing boundary ([ApiScope.OPEN] or [ApiScope.MANAGEMENT])
 * through which the socket was connected.
 * @property userId Unique identifier of the authenticated user, or `null` if the connection is anonymous.
 * @property userRole Security role assigned to the authenticated user, or `null` if unauthenticated.
 * @property userSessionId Identifier of the active server session associated with the access token, or `null`.
 * @property clientInfo Hardware, OS, and platform metadata extracted from request headers and enriched
 * during the client handshake sequence.
 */
data class WebSocketSessionContext(
    val socketSessionId: String,
    val apiScope: ApiScope,
    val userId: UserId?,
    val userRole: UserRole?,
    val userSessionId: UserSessionId?,
    var clientInfo: ClientInfo?
)