package io.github.mudrichenkoevgeny.backend.feature.user.network.request

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.client.createTestClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

fun createTestAuthenticatedRequestContext(
    traceId: String? = null,
    userId: UserId = UserId.generate(),
    userRole: UserRole = UserRole.ADMIN,
    sessionId: UserSessionId = UserSessionId.generate(),
    clientInfo: ClientInfo = createTestClientInfo()
): AuthenticatedRequestContext = AuthenticatedRequestContext(
    traceId = traceId,
    userId = userId,
    userRole = userRole,
    sessionId = sessionId,
    clientInfo = clientInfo
)
