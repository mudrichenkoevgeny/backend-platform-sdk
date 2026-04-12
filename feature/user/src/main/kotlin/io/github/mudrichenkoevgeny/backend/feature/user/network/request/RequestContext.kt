package io.github.mudrichenkoevgeny.backend.feature.user.network.request

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Per-request context extracted from incoming HTTP calls.
 *
 * Carries identifiers used for tracing, authorization and client analytics,
 * and is typically propagated through handlers and use cases instead of
 * passing raw framework types.
 *
 * @param traceId correlation id for logs and distributed tracing.
 * @param userId authenticated user identifier, if available.
 * @param userRole effective [UserRole] of the authenticated user, if available; used for authorization checks.
 * @param sessionId authenticated session identifier, if available.
 * @param clientInfo structured information about the calling client.
 */
data class RequestContext(
    val traceId: String?,
    val userId: UserId?,
    val userRole: UserRole?,
    val sessionId: UserSessionId?,
    val clientInfo: ClientInfo
)