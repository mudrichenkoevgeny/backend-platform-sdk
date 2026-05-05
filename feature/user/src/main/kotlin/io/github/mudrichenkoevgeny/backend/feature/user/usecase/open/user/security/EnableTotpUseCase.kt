package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessor
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
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
class EnableTotpUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val totpManager: TotpManager,
    private val mfaService: MfaService,
    private val totpCryptoProcessor: TotpCryptoProcessor
) {
    /**
     * Finalizes TOTP activation by verifying the first code from the authenticator app.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.USER_ENABLE_TOTP].
     * 2. Validates the provided [mfaToken] and ensures it matches the current session.
     * 3. Retrieves pending TOTP settings and verifies that the provided [code] is valid.
     * 4. Generates an initial set of recovery codes via [TotpCryptoProcessor].
     * 5. Marks TOTP as enabled and stores the recovery codes via [TotpManager].
     * 6. Consumes the MFA challenge to prevent reuse.
     * 7. Logs the completion via [AuditLogger] with [UserAuditActionType.SELF_ENABLE_TOTP].
     *
     * @param authenticatedRequestContext The context of the authenticated request.
     * @param mfaToken The token obtained during the [SetupTotpUseCase] step.
     * @param code The 6-digit TOTP code from the user's authenticator app.
     * @return [AppResult] containing the initial [TotpRecoveryCodes].
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext,
        mfaToken: String,
        code: String
    ): AppResult<TotpRecoveryCodes> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()
        val currentUserId = authenticatedRequestContext.userId

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.USER_ENABLE_TOTP,
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

        val user = when (userResult) {
            is AppResult.Error -> return handleError(
                error = userResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> userResult.data
        }

        if (user.isTotpEnabled) {
            return handleError(
                error = SecurityError.TotpAlreadyEnabled(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val mfaChallengeResult = mfaService.getChallenge(
            token = mfaToken,
            type = MfaChallengeType.SETUP_TOTP
        )
        val mfaChallenge = when (mfaChallengeResult) {
            is AppResult.Error -> return handleError(
                error = mfaChallengeResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> mfaChallengeResult.data
        }

        if (mfaChallenge.sessionId != authenticatedRequestContext.sessionId.asHexDashString()) {
            return handleError(
                error = SecurityError.InvalidMfaToken(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val settingsResult = totpManager.getSettings(currentUserId)
            .mapNotNullOrError(SecurityError.TotpNotEnabled())

        val settings = when (settingsResult) {
            is AppResult.Error -> return handleError(
                error = settingsResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> settingsResult.data
        }

        if (settings.isConfirmed) {
            return handleError(
                error = SecurityError.TotpAlreadyEnabled(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val validationResult = totpCryptoProcessor.isCodeValid(
            code = code,
            encryptedSecret = settings.encryptedSecret
        )
        val isValid = when (validationResult) {
            is AppResult.Error -> return handleError(
                error = validationResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> validationResult.data
        }

        if (!isValid) {
            return handleError(
                error = SecurityError.InvalidTotpCode(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val recoveryCodesResult = totpCryptoProcessor.generateRecoveryCodes()
        val decryptedRecoveryCodes = when (recoveryCodesResult) {
            is AppResult.Error -> return handleError(
                error = recoveryCodesResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> recoveryCodesResult.data
        }

        val confirmResult = totpManager.confirmTotp(
            userId = currentUserId,
            decryptedRecoveryCodes = decryptedRecoveryCodes
        )
        if (confirmResult is AppResult.Error) {
            return handleError(
                error = confirmResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val consumeResult = mfaService.consumeChallenge(mfaToken)
        if (consumeResult is AppResult.Error) {
            return handleError(
                error = consumeResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
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
            action = UserAuditActionType.SELF_ENABLE_TOTP,
            resource = UserAuditResourceType.USER,
            resourceId = actorId,
            status = status,
            metadata = metadata
        )
    }
}