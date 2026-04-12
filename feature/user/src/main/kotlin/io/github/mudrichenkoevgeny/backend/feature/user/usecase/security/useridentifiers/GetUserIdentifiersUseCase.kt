package io.github.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers

import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.audit.PlatformUserAuditActionTypeExtension
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: list all authentication identifiers for the current user.
 *
 * Requires userId in request context. Delegates to [UserIdentifierManager.getUserIdentifierListByUserId] and logs audit on success.
 * [execute] takes request context;
 * returns [AppResult.Success] with list of [UserIdentifier] or [AppResult.Error] (e.g. [UserError.InvalidAccessToken]).
 */
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
                resourceId = userId.asHexDashString()
            )
        }

        return userIdentifiersListResult
    }

    companion object {
        const val AUDIT_ACTION = PlatformUserAuditActionTypeExtension.SERIAL_GET_USER_IDENTIFIERS
        const val AUDIT_RESOURCE = UserAuditResourceType.RESOURCE_USER
    }
}