package com.mudrichenkoevgeny.backend.feature.user.network.request.context

import com.mudrichenkoevgeny.backend.core.common.constants.NetworkConstants
import com.mudrichenkoevgeny.backend.core.common.constants.TracingConstants
import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.network.request.model.extractClientInfo
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.getJWTPrincipal
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.getSessionId
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserId
import io.ktor.server.routing.RoutingCall
import org.slf4j.MDC

fun RoutingCall.getRequestContext(): RequestContext {
    val traceId = request.headers[NetworkConstants.TRACE_HEADER_NAME]
        ?: MDC.get(TracingConstants.TRACE_ID_KEY)

    val principal = this.getJWTPrincipal()

    return RequestContext(
        traceId = traceId,
        userId = principal?.getUserId(),
        sessionId = principal?.getSessionId(),
        clientInfo = this.extractClientInfo()
    )
}