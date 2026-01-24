package com.mudrichenkoevgeny.backend.core.common.network.request.model

import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.model.UserSessionId

data class RequestContext(
    val traceId: String?,
    val userId: UserId?,
    val sessionId: UserSessionId?,
    val clientInfo: ClientInfo
)