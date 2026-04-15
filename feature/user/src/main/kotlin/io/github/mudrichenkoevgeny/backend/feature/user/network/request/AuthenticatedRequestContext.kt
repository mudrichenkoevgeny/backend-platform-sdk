package io.github.mudrichenkoevgeny.backend.feature.user.network.request

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Per-request context extracted from incoming HTTP calls for authenticated users.
 *
 * Carries identifiers used for tracing, authorization and client analytics,
 * and guarantees the presence of user-related fields extracted from a JWT
 * principal.
 *
 * @param traceId correlation id for logs and distributed tracing.
 * @param userId authenticated user identifier extracted from JWT.
 * @param userRole effective [UserRole] of the authenticated user; used for authorization checks.
 * @param sessionId authenticated session identifier extracted from JWT.
 * @param clientInfo structured information about the calling client.
 */
data class AuthenticatedRequestContext(
    val traceId: String?,
    val userId: UserId,
    val userRole: UserRole,
    val sessionId: UserSessionId,
    val clientInfo: ClientInfo
)