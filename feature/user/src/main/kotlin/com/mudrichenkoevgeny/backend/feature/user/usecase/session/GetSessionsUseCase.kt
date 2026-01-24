package com.mudrichenkoevgeny.backend.feature.user.usecase.session

import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import com.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import javax.inject.Inject
import javax.inject.Singleton

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
                resourceId = userId.asString()
            )
        }

        return userSessionsResult
    }

    companion object {
        const val AUDIT_ACTION = "get_sessions"
        const val AUDIT_RESOURCE = "user"
    }
}