package com.mudrichenkoevgeny.backend.feature.user.usecase.user

import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import com.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import com.mudrichenkoevgeny.backend.feature.user.model.user.UserData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserUseCase @Inject constructor(
    private val userAuditLogger: UserAuditLogger,
    private val userManager: UserManager,
    private val userIdentifierManager: UserIdentifierManager
) {
    suspend fun execute(
        userId: UserId,
        requestContext: RequestContext
    ): AppResult<UserData> {
        val currentUserId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val auditResourceId = currentUserId.asString()

        val auditMetadata = mapOf(UserAuditMetadata.Keys.USER_ID to currentUserId.asString())

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

        val userIdentifiersListResult = userIdentifierManager.getUserIdentifierListByUserId(currentUserId)

        val userIdentifiersList = if (userIdentifiersListResult is AppResult.Success) {
            userIdentifiersListResult.data
        } else {
            emptyList()
        }

        userAuditLogger.logSuccess(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            metadata = auditMetadata
        )

        return AppResult.Success(
            UserData(
                user = user,
                userIdentifiersList = userIdentifiersList
            )
        )
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