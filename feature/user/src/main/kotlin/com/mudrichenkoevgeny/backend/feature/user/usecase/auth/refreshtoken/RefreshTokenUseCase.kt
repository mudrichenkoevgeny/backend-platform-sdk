package com.mudrichenkoevgeny.backend.feature.user.usecase.auth.refreshtoken

import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.SessionToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshTokenUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val sessionManager: SessionManager
) {
    suspend fun execute(
        refreshToken: RefreshToken,
        requestContext: RequestContext
    ): AppResult<SessionToken> {
        val auditResourceId = requestContext.sessionId?.value.toString()
        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.REFRESH_TOKEN,
            rateLimitIdentifier = refreshToken.value,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val refreshSessionResult = sessionManager.refreshSession(
            userId = requestContext.userId,
            refreshToken = refreshToken,
            clientInfo = requestContext.clientInfo
        )

        when (refreshSessionResult) {
            is AppResult.Success -> {
                userAuditLogger.logSuccess(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId,
                    type = UserAuditMetadata.Types.VERIFICATION_CODE_SENT
                )
            }
            is AppResult.Error -> {
                if (refreshSessionResult.error is UserError.InvalidRefreshToken) {
                    userAuditLogger.logFail(
                        requestContext = requestContext,
                        action = AUDIT_ACTION,
                        resource = AUDIT_RESOURCE,
                        resourceId = auditResourceId,
                        type = UserAuditMetadata.Types.INVALID_REFRESH_TOKEN
                    )
                } else {
                    userAuditLogger.logInternalError(
                        requestContext = requestContext,
                        action = AUDIT_ACTION,
                        resource = AUDIT_RESOURCE,
                        resourceId = auditResourceId
                    )
                }
            }
        }

        return refreshSessionResult
    }

    companion object {
        const val AUDIT_ACTION = "refresh_token"
        const val AUDIT_RESOURCE = "session"
    }
}