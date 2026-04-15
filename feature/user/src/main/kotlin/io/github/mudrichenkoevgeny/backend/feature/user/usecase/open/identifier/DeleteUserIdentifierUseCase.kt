package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.audit.toDeniedSecurityAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toErrorUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.plus

// todo refactor
/**
 * Use case: delete one of the current user's authentication identifiers.
 *
 * The identifier must not be the one used by the current session; the user must have at least two identifiers.
 * Requires recent authentication confirmation. Applies rate limiting, then delegates to [IdentifierManager.deleteUserIdentifier].
 * returns [AppResult.Success] or [AppResult.Error] (e.g. [UserError.CannotDeleteUserIdentifier], [SecurityError.AuthenticationConfirmationRequired]).
 */
@Singleton
class DeleteUserIdentifierUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val sessionManager: SessionManager,
    private val identifierManager: IdentifierManager
) {
    suspend operator fun invoke(
        userIdentifierId: UserIdentifierId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = userIdentifierId.asHexDashString()
        val currentUserId = authenticatedRequestContext.userId
        val currentSessionId = authenticatedRequestContext.sessionId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + setOf(
            AuditEventMetadata(
                key = UserAuditMetadataKey.SESSION_ID,
                value = currentSessionId.asHexDashString()
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.USER_IDENTIFIER_CHANGE,
            identifier = currentUserId.asHexDashString()
        )

        if (rateLimitCheck is AppResult.Error) {
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + rateLimitCheck.error.toDeniedSecurityAuditEventMetadata()
            )
            return rateLimitCheck
        }

        val userIdentifiersListResult = identifierManager.getUserIdentifiersByUserId(currentUserId)

        val userIdentifiersList = when (userIdentifiersListResult) {
            is AppResult.Success -> userIdentifiersListResult.data
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + userIdentifiersListResult.error.toErrorUserAuditEventMetadata()
                )
                return userIdentifiersListResult
            }
        }

        val userSessionResult = sessionManager.getUserSessionById(currentSessionId)

        val currentSession = when (userSessionResult) {
            is AppResult.Success -> userSessionResult.data
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + userSessionResult.error.toErrorUserAuditEventMetadata()
                )
                return userSessionResult
            }
        }

        val currentSessionIdentifierId = userIdentifiersList
            .find { userIdentifier -> userIdentifier.id == currentSession?.identifierId }?.id

        val identifierIdToDelete = userIdentifiersList
            .find { userIdentifier -> userIdentifier.id == userIdentifierId }?.id

        if (currentSession == null
            || currentSession.userId != currentUserId
            || currentSessionIdentifierId == null
            || identifierIdToDelete == null
            || userIdentifiersList.size < 2
        ) {
            val error = UserError.CannotDeleteUserIdentifier()
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + error.toDeniedUserAuditEventMetadata()
            )
            return AppResult.Error(error)
        }

        /*
        // todo wait for authentication confirmation release
        val isAuthenticationConfirmedRecently = authenticationPolicyChecker.isAuthenticationConfirmedRecently(
            lastReauthenticatedAt = currentSession.lastReauthenticatedAt
        )

        if (!isAuthenticationConfirmedRecently) {
            val error = SecurityError.AuthenticationConfirmationRequired()
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + error.toDeniedUserAuditEventMetadata()
            )
            return AppResult.Error(error)
        }
        */

        val deleteUserIdentifierResult = identifierManager.deleteUserIdentifier(
            userIdentifierId = identifierIdToDelete
        )

        when (deleteUserIdentifierResult) {
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + deleteUserIdentifierResult.error.toErrorUserAuditEventMetadata()
                )
            }
            is AppResult.Success -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
        }

        return deleteUserIdentifierResult
    }

    private fun logAudit(
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        resourceId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.SELF_DELETE_IDENTIFIER,
            resource = UserAuditResourceType.USER_IDENTIFIER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}
