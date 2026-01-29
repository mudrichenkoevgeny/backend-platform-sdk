package io.github.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserIdentifiersUseCase @Inject constructor(
    private val userAuditLogger: UserAuditLogger,
    private val userIdentifierManager: UserIdentifierManager
) {
    suspend fun execute(
        requestContext: RequestContext
    ): AppResult<List<UserIdentifier>> {
        val userId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val userIdentifiersListResult = userIdentifierManager.getUserIdentifierListByUserId(userId)

        if (userIdentifiersListResult is AppResult.Success) {
            userAuditLogger.logSuccess(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = userId.asString()
            )
        }

        return userIdentifiersListResult
    }

    companion object {
        const val AUDIT_ACTION = "get_user_identifiers"
        const val AUDIT_RESOURCE = "user"
    }
}