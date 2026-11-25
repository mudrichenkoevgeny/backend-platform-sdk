package io.github.mudrichenkoevgeny.backend.feature.user.network.request

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.common.domain.model.client.createTestClientInfo

fun createTestRequestContext(
    traceId: String? = null,
    userId: UserId? = null,
    userRole: UserRole? = null,
    sessionId: UserSessionId? = null,
    clientInfo: ClientInfo = createTestClientInfo()
): RequestContext = RequestContext(
    traceId = traceId,
    userId = userId,
    userRole = userRole,
    sessionId = sessionId,
    clientInfo = clientInfo
)
