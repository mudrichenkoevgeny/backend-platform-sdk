package io.github.mudrichenkoevgeny.backend.feature.user.usecase.session

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.audit.PlatformUserAuditActionTypeExtension
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: list all active sessions for the current user.
 *
 * Requires userId in request context. Delegates to [SessionManager] and logs audit on success.
 * [execute] takes request context;
 * returns [AppResult.Success] with list of [UserSession] or [AppResult.Error] (e.g. [UserError.InvalidAccessToken]).
 */
@Singleton
class GetSessionsUseCase @Inject constructor(
    private val userAuditLogger: UserAuditLogger,
    private val sessionManager: SessionManager
) {
    suspend fun execute(
        requestContext: RequestContext
    ): AppResult<List<UserSession>> {
        val userId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val userSessionsResult = sessionManager.getAllUserSessions(userId)

        if (userSessionsResult is AppResult.Success) {
            userAuditLogger.logSuccess(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = userId.asHexDashString()
            )
        }

        return userSessionsResult
    }

    companion object {
        const val AUDIT_ACTION = PlatformUserAuditActionTypeExtension.SERIAL_GET_SESSIONS
        const val AUDIT_RESOURCE = UserAuditResourceType.RESOURCE_USER
    }
}