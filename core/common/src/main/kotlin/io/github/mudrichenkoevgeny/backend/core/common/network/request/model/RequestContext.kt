package io.github.mudrichenkoevgeny.backend.core.common.network.request.model

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId

data class RequestContext(
    val traceId: String?,
    val userId: UserId?,
    val sessionId: UserSessionId?,
    val clientInfo: ClientInfo
)