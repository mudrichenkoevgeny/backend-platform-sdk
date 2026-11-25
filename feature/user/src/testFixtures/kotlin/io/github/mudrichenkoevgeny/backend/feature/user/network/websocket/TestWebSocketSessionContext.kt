package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket

import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserSessionId

fun createTestWebSocketSessionContext(
    socketSessionId: String = "socket-id",
    apiScope: ApiScope = ApiScope.OPEN,
    userId: UserId? = null,
    userRole: UserRole? = null,
    clientInfo: ClientInfo? = null,
    userSessionId: UserSessionId? = null
) = WebSocketSessionContext(
    socketSessionId = socketSessionId,
    apiScope = apiScope,
    userId = userId,
    userRole = userRole,
    clientInfo = clientInfo,
    userSessionId = userSessionId
)
