package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

// todo refactor
/**
 * Use case: revoke all sessions for the current user except the current one.
 *
 * Requires userId and sessionId in request context. Applies rate limiting, then [SessionManager.revokeAllUserSessionsExceptOne].
 * [execute] takes request context;
 * returns [AppResult.Success] or [AppResult.Error] (e.g. [UserError.InvalidAccessToken], rate limit).
 */
@Singleton
class DeleteAllOtherSessionsUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val sessionManager: SessionManager
) {
    suspend fun execute(
        requestContext: RequestContext
    ): AppResult<Unit> {
        val userId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val auditResourceId = userId.asHexDashString()

        val sessionId = requestContext.sessionId
            ?: return AppResult.Error(UserError.InvalidSession())

        val auditMetadata = mapOf(UserAuditMetadata.Keys.SESSION_ID to sessionId.asHexDashString())

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = UserRateLimitAction.LOGOUT_ATTEMPT,
            rateLimitIdentifier = sessionId.asHexDashString(),
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val revokeSessionResult = sessionManager.revokeAllUserSessionsExceptOne(userId, sessionId)

        when (revokeSessionResult) {
            is AppResult.Success -> {
                userAuditLogger.logSuccess(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId,
                    metadata = auditMetadata
                )
            }
            is AppResult.Error -> {
                userAuditLogger.logInternalError(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId,
                    metadata = auditMetadata
                )
            }
        }

        return revokeSessionResult
    }

    companion object {
        const val AUDIT_ACTION = UserAuditActionType.ACTION_SELF_REVOKE_OTHER_SESSIONS
        const val AUDIT_RESOURCE = UserAuditResourceType.RESOURCE_USER
    }
}