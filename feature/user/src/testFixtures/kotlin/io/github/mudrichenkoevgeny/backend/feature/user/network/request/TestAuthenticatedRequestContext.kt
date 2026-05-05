package io.github.mudrichenkoevgeny.backend.feature.user.network.request

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

fun createTestAuthenticatedRequestContext(
    userId: UserId = UserId.generate(),
    role: UserRole = UserRole.ADMIN,
    traceId: String = "test-trace-id"
) = AuthenticatedRequestContext(
    traceId = traceId,
    userId = userId,
    userRole = role,
    sessionId = UserSessionId.generate(),
    clientInfo = ClientInfo()
)