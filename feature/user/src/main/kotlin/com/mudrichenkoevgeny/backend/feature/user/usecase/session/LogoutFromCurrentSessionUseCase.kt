package com.mudrichenkoevgeny.backend.feature.user.usecase.session

import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogoutFromCurrentSessionUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val sessionManager: SessionManager
) {
    suspend fun execute(
        requestContext: RequestContext
    ): AppResult<Unit> {
        val auditResourceId = requestContext.userId?.asString()

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

        val revokeSessionResult = sessionManager.revokeSessionById(sessionId)

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
        const val AUDIT_ACTION = "logout_from_current_session"
        const val AUDIT_RESOURCE = "user"
    }
}