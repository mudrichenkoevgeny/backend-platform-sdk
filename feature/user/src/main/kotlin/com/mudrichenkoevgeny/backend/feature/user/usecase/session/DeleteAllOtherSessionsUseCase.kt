package io.github.mudrichenkoevgeny.backend.feature.user.usecase.session

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

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

        val auditResourceId = userId.asString()

        val sessionId = requestContext.sessionId
            ?: return AppResult.Error(UserError.InvalidSession())

        val auditMetadata = mapOf(UserAuditMetadata.Keys.SESSION_ID to sessionId.asString())

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.LOGOUT_ATTEMPT,
            rateLimitIdentifier = sessionId.asString(),
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
        const val AUDIT_ACTION = "delete_all_other_sessions"
        const val AUDIT_RESOURCE = "user"
    }
}