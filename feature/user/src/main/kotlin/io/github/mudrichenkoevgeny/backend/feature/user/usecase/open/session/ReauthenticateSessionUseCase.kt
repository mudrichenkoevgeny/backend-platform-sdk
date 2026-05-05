package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReauthenticateSessionUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val mfaService: MfaService,
    private val sessionManager: SessionManager,
    private val totpManager: TotpManager
) {
    /**
     * Performs session re-authentication (Step-up) via TOTP to elevate the trust level.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.SESSION_REAUTHENTICATE].
     * 2. Validates the [mfaToken] challenge of type [MfaChallengeType.STEP_UP].
     * 3. Verifies the user's identity by checking the provided TOTP [code].
     * 4. Consumes the MFA challenge and updates the session's `lastReauthenticatedAt` timestamp.
     * 5. Logs the re-authentication via [AuditLogger] with [UserAuditActionType.REAUTHENTICATE_SESSION].
     *
     * @param authenticatedRequestContext The context of the authenticated request.
     * @param mfaToken The token associated with the security challenge.
     * @param code TOTP code for verification.
     * @return [AppResult] indicating successful session elevation.
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext,
        mfaToken: String,
        code: String
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        val sessionId = authenticatedRequestContext.sessionId

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.SESSION_REAUTHENTICATE,
            identifier = auditActorId
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                sessionId = sessionId.asHexDashString(),
                baseMetadata = auditMetadata
            )
        }

        val getChallengeResult = mfaService.getChallenge(
            token = mfaToken,
            type = MfaChallengeType.STEP_UP
        )
        val mfaChallengeData = when (getChallengeResult) {
            is AppResult.Error -> return handleError(
                error = getChallengeResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                sessionId = sessionId.asHexDashString(),
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> getChallengeResult.data
        }

        if (mfaChallengeData.userId != authenticatedRequestContext.userId.asHexDashString()
            || mfaChallengeData.sessionId != sessionId.asHexDashString()
        ) {
            return handleError(
                error = SecurityError.InvalidMfaToken(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                sessionId = sessionId.asHexDashString(),
                baseMetadata = auditMetadata
            )
        }

        val verifyResult = totpManager.verifyTotp(
            userId = authenticatedRequestContext.userId,
            code = code
        )
        if (verifyResult is AppResult.Error) {
            return handleError(
                error = verifyResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                sessionId = sessionId.asHexDashString(),
                baseMetadata = auditMetadata
            )
        }

        mfaService.consumeChallenge(mfaToken)

        val updateResult = sessionManager.updateLastReauthenticated(sessionId)
        if (updateResult is AppResult.Error) {
            return handleError(
                error = updateResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                sessionId = sessionId.asHexDashString(),
                baseMetadata = auditMetadata
            )
        }

        logAudit(
            actorId = auditActorId,
            actorUserRole = auditActorUserRole,
            sessionId = sessionId.asHexDashString(),
            status = AuditStatus.SUCCESS,
            metadata = auditMetadata
        )

        return AppResult.Success(Unit)
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole?,
        sessionId: String,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            actorUserRole = actorUserRole,
            sessionId = sessionId,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String,
        actorUserRole: UserRole?,
        sessionId: String,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.REAUTHENTICATE_SESSION,
            resource = UserAuditResourceType.SESSION,
            resourceId = sessionId,
            status = status,
            metadata = metadata
        )
    }
}