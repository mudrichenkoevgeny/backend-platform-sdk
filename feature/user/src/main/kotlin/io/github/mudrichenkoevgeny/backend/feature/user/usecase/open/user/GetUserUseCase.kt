package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import javax.inject.Inject
import javax.inject.Singleton

// todo refactor
/**
 * Use case: load the current user profile.
 *
 * Ensures the requester can only read their own user (userId must match the authenticated user from request context).
 * Delegates to [UserManager] and records audit events on success or failure.
 * [execute] takes userId and request context;
 * returns [AppResult.Success] with [User] or [AppResult.Error] (e.g. [UserError.InvalidAccessToken], [UserError.UserNotFound]).
 */
@Singleton
class GetUserUseCase @Inject constructor(
    private val userAuditLogger: UserAuditLogger,
    private val userManager: UserManager
) {
    suspend fun execute(
        userId: UserId,
        requestContext: RequestContext
    ): AppResult<User> {
        val currentUserId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val auditResourceId = currentUserId.asHexDashString()

        val auditMetadata = mapOf(UserAuditMetadata.Keys.USER_ID to currentUserId.asHexDashString())

        if (userId != currentUserId) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.CAN_NOT_GET_USER,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.InvalidAccessToken())
        }

        val getUserResult = userManager.getUserById(userId)
            .mapNotNullOrError(UserError.UserNotFound(userId))

        val user = when (getUserResult) {
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return getUserResult
            }
            is AppResult.Success -> {
                getUserResult.data
            }
        }

        userAuditLogger.logSuccess(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            metadata = auditMetadata
        )

        return AppResult.Success(user)
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
        const val AUDIT_ACTION = "get_user"
        const val AUDIT_RESOURCE = "user"
    }
}