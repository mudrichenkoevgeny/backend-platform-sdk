package com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers

import com.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import com.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteUserIdentifierUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val sessionManager: SessionManager,
    private val userIdentifierManager: UserIdentifierManager,
    private val authenticationPolicyChecker: AuthenticationPolicyChecker
) {
    suspend fun execute(
        userIdentifierId: UserIdentifierId,
        requestContext: RequestContext
    ): AppResult<Unit> {
        val userId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val currentSessionId = requestContext.sessionId
            ?: return AppResult.Error(UserError.InvalidSession())

        val auditResourceId = userId.asString()
        val auditMetadata = mapOf(UserAuditMetadata.Keys.USER_IDENTIFIER_ID to userIdentifierId.asString())

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.USER_IDENTIFIER_CHANGE,
            rateLimitIdentifier = userIdentifierId.asString(),
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val userIdentifiersListResult = userIdentifierManager.getUserIdentifierListByUserId(userId)

        val userIdentifiersList = when (userIdentifiersListResult) {
            is AppResult.Success -> userIdentifiersListResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return userIdentifiersListResult
            }
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
            return AppResult.Error(UserError.CannotDeleteUserIdentifier())
        }

        val currentSessionIdentifier = userIdentifiersList
            .find { userIdentifier -> userIdentifier.id == currentSession.userIdentifierId }

        val sessionIdentifierToDelete = userIdentifiersList
            .find { userIdentifier -> userIdentifier.id == userIdentifierId }

        if (currentSessionIdentifier == null || sessionIdentifierToDelete == null) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
            )
            return AppResult.Error(UserError.CannotDeleteUserIdentifier())
        }

        if (userIdentifiersList.size < 2 || currentSessionIdentifier.id == sessionIdentifierToDelete.id) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.CAN_NOT_DELETE_USER_IDENTIFIER,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.CannotDeleteUserIdentifier())
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

        val deleteUserIdentifierResult = userIdentifierManager.deleteUserIdentifier(sessionIdentifierToDelete.id)

        when (deleteUserIdentifierResult) {
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

        return deleteUserIdentifierResult
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
        const val AUDIT_ACTION = "delete_user_identifier"
        const val AUDIT_RESOURCE = "user"
    }
}