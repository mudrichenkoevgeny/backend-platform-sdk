package com.mudrichenkoevgeny.backend.feature.user.usecase.user

import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import com.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteUserUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val sessionManager: SessionManager,
    private val userManager: UserManager,
    private val authenticationPolicyChecker: AuthenticationPolicyChecker
) {
    suspend fun execute(
        userId: UserId,
        requestContext: RequestContext
    ): AppResult<Unit> {
        val currentUserId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val currentSessionId = requestContext.sessionId
            ?: return AppResult.Error(UserError.InvalidSession())

        val auditResourceId = currentUserId.asString()

        val auditMetadata = mapOf(UserAuditMetadata.Keys.USER_ID to currentUserId.asString())

        if (userId != currentUserId) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.CAN_NOT_DELETE_USER,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.InvalidAccessToken())
        }

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.USER_DELETE,
            rateLimitIdentifier = currentUserId.asString(),
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val userSessionsResult = sessionManager.getAllUserSessions(userId)

        val userSessions = when (userSessionsResult) {
            is AppResult.Success -> userSessionsResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return userSessionsResult
            }
        }

        val currentSession = userSessions.find { userSession -> userSession.id == currentSessionId }

        if (currentSession == null) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
            )
            return AppResult.Error(UserError.InvalidSession())
        }

        val isAuthenticationConfirmedRecently = authenticationPolicyChecker.isAuthenticationConfirmedRecently(
            lastReauthenticatedAt = currentSession.lastReauthenticatedAt
        )

        if (!isAuthenticationConfirmedRecently) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.AUTHENTICATION_CONFIRMATION_REQUIRED,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.AuthenticationConfirmationRequired())
        }

        val deleteUserResult = userManager.deleteUserById(userId)

        when (deleteUserResult) {
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
            }
            is AppResult.Success -> {
                userAuditLogger.logSuccess(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId,
                    metadata = auditMetadata
                )
            }
        }

        return deleteUserResult
    }

    private fun logAuditInternalError(
        requestContext: RequestContext,
        auditResourceId: String?,
        auditMetadata: Map<String, String>
    ) {
        userAuditLogger.logInternalError(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            metadata = auditMetadata
        )
    }

    companion object {
        const val AUDIT_ACTION = "delete_user"
        const val AUDIT_RESOURCE = "user"
    }
}