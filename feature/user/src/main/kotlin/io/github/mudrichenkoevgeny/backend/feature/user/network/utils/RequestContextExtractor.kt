package io.github.mudrichenkoevgeny.backend.feature.user.network.utils

import io.github.mudrichenkoevgeny.backend.core.common.logs.naming.TracingKeys
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.extractClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getJWTPrincipal
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getSessionId
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserId
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserRole
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
import io.ktor.server.routing.RoutingCall
import org.slf4j.MDC

/**
 * Builds a [RequestContext] for the current call.
 *
 * The trace id is resolved in the following order:
 * - request header ([CommonHttpHeaders.TRACE_HEADER_NAME])
 * - logging MDC ([TracingKeys.TRACE_ID_KEY])
 *
 * Auth-related fields are populated from the JWT principal when present.
 */
fun RoutingCall.getRequestContext(): RequestContext {
    val traceId = request.headers[CommonHttpHeaders.TRACE_HEADER_NAME]
        ?: MDC.get(TracingKeys.TRACE_ID_KEY)

    val principal = this.getJWTPrincipal()

    return RequestContext(
        traceId = traceId,
        userId = principal?.getUserId(),
        userRole = principal?.getUserRole(),
        sessionId = principal?.getSessionId(),
        clientInfo = this.extractClientInfo()
    )
}