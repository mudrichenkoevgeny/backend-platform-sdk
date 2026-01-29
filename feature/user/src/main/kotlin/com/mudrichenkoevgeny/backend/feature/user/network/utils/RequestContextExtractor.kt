package io.github.mudrichenkoevgeny.backend.feature.user.network.utils

import io.github.mudrichenkoevgeny.backend.core.common.constants.NetworkConstants
import io.github.mudrichenkoevgeny.backend.core.common.constants.TracingConstants
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.extractClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getJWTPrincipal
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getSessionId
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserId
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