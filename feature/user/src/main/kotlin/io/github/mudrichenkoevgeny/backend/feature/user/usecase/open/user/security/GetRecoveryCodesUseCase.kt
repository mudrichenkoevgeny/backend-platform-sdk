package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.totprecoverycodes.TotpRecoveryCodes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetRecoveryCodesUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val totpManager: TotpManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Retrieves the current active recovery codes for the authenticated user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Security:**
     * - Sensitive operation.
     * - Requires MFA Step-up (session confirmation) via [AuthenticationChallengeService].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.USER_GET_RECOVERY_CODES].
     * 2. Validates that the user has TOTP enabled.
     * 3. Ensures the session is recently confirmed (Step-up).
     * 4. Fetches and decrypts existing recovery codes via [TotpManager].
     * 5. Logs the access via [AuditLogger] with [UserAuditActionType.SELF_GET_RECOVERY_CODES].
     *
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing the current [TotpRecoveryCodes].
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<TotpRecoveryCodes> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()
        val currentUserId = authenticatedRequestContext.userId

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.USER_GET_RECOVERY_CODES,
            identifier = auditActorId
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val userResult = userManager.getUserByIdForSelf(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (userResult) {
            is AppResult.Error -> return handleError(
                error = userResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> userResult.data
        }

        if (!currentUser.isTotpEnabled) {
            return handleError(
                error = SecurityError.TotpNotEnabled(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val userSessionResult = sessionManager.getUserSessionForSystem(
            userSessionId = authenticatedRequestContext.sessionId
        ).mapNotNullOrError(UserError.InvalidSession())

        val currentSession = when (userSessionResult) {
            is AppResult.Success -> userSessionResult.data
            is AppResult.Error -> return handleError(
                error = userSessionResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val ensureSessionConfirmedResult = authenticationChallengeService.ensureSessionConfirmed(
            userDetails = currentUser,
            userSession = currentSession
        )
        if (ensureSessionConfirmedResult is AppResult.Error) {
            return handleError(
                error = ensureSessionConfirmedResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val getDecryptedRecoveryCodesResult = totpManager.getDecryptedRecoveryCodes(currentUserId)
        val decryptedRecoveryCodes = when (getDecryptedRecoveryCodesResult) {
            is AppResult.Error -> return handleError(
                error = getDecryptedRecoveryCodesResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> getDecryptedRecoveryCodesResult.data
        }

        logAudit(
            actorId = auditActorId,
            actorUserRole = auditActorUserRole,
            status = AuditStatus.SUCCESS,
            metadata = auditMetadata
        )

        return AppResult.Success(
            TotpRecoveryCodes(
                codes = decryptedRecoveryCodes.map { decryptedRecoveryCode ->
                    decryptedRecoveryCode.value
                }
            )
        )
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole?,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            actorUserRole = actorUserRole,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String,
        actorUserRole: UserRole?,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.SELF_GET_RECOVERY_CODES,
            resource = UserAuditResourceType.USER,
            resourceId = actorId,
            status = status,
            metadata = metadata
        )
    }
}