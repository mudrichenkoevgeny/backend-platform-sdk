package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.validation.validateRoleAndStatus
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockByPhoneUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val identifierManager: IdentifierManager,
    private val otpService: OtpService,
    private val userManager: UserManager
) {
    /**
     * Unlocks a temporarily locked user account using a phone confirmation code.
     *
     * **Allowed Account Statuses:** Any (Self-service unlock flow).
     *
     * **Security:**
     * - Checks if self-service unlock is enabled via [SecuritySettingsProvider].
     * - Enforces rate limits on unlock attempts.
     * - Verifies the confirmation code via [OtpService] with [UserOtpVerificationType.PHONE_UNLOCK].
     *
     * **Workflow:**
     * 1. Confirms self-service unlock is enabled in account lockout policy.
     * 2. Enforces rate limits for the provided phone number.
     * 3. Verifies the OTP code via [OtpService].
     * 4. Resolves the phone identifier via [IdentifierManager].
     * 5. Unlocks the user account via [UserManager.unlockUserAccount].
     * 6. Logs the security audit event via [AuditLogger] with [UserAuditActionType.SELF_UNLOCK_ACCOUNT].
     *
     * @param phoneNumber Target phone number in E.164 format.
     * @param confirmationCode Verification code received via SMS.
     * @param requestContext Request context containing client details.
     * @return [AppResult.Success] with [Unit] upon successful unlock, or an error.
     */
    suspend operator fun invoke(
        phoneNumber: String,
        confirmationCode: String,
        requestContext: RequestContext,
        allowedRoles: Set<UserRole> = UserRole.entries.toSet(),
        allowedAccountStatuses: Set<UserAccountStatus> = UserAccountStatus.entries.toSet()
    ): AppResult<Unit> {
        val auditMetadata = requestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.PHONE_NUMBER,
                value = phoneNumber
            )
        )

        val accountLockoutPolicy = securitySettingsProvider.getAccountLockoutPolicy()
        if (!accountLockoutPolicy.isSelfServiceUnlockEnabled) {
            return handleError(
                error = UserError.SelfServiceUnlockDisabled(),
                baseMetadata = auditMetadata
            )
        }

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.LOGIN_ATTEMPT,
            identifier = phoneNumber
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                baseMetadata = auditMetadata
            )
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = phoneNumber,
            type = UserOtpVerificationType.PHONE_UNLOCK,
            code = confirmationCode
        )

        val isConfirmationCodeCorrect = when (verifyOtpResult) {
            is AppResult.Success -> verifyOtpResult.data
            is AppResult.Error -> return handleError(
                error = verifyOtpResult.error,
                baseMetadata = auditMetadata
            )
        }

        if (!isConfirmationCodeCorrect) {
            return handleError(
                error = UserError.WrongConfirmationCode(),
                baseMetadata = auditMetadata
            )
        }

        val identifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = phoneNumber
        ).mapNotNullOrError(UserError.UserNotFound())

        val userIdentifier = when (identifierResult) {
            is AppResult.Success -> identifierResult.data
            is AppResult.Error -> return handleError(
                error = identifierResult.error,
                baseMetadata = auditMetadata
            )
        }

        val userResult = userManager.getUserByIdForSelf(userIdentifier.userId)
            .mapNotNullOrError(UserError.UserNotFound())
        val user = when (userResult) {
            is AppResult.Success -> userResult.data
            is AppResult.Error -> return handleError(
                error = userResult.error,
                baseMetadata = auditMetadata
            )
        }

        user.validateRoleAndStatus(allowedRoles, allowedAccountStatuses)?.let { error ->
            return handleError(
                error = error,
                actorId = user.id.asHexDashString(),
                resourceId = user.id.asHexDashString(),
                baseMetadata = auditMetadata
            )
        }

        val unlockResult = userManager.unlockUserAccount(
            userId = userIdentifier.userId,
            clearLockoutForIdentifiers = listOf(phoneNumber)
        )
        return when (unlockResult) {
            is AppResult.Error -> handleError(
                error = unlockResult.error,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> {
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = userIdentifier.userId.asHexDashString(),
                    resourceId = userIdentifier.userId.asHexDashString(),
                    metadata = auditMetadata
                )
                AppResult.Success(Unit)
            }
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String? = null,
        resourceId: String? = null,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            resourceId = resourceId,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String? = null,
        resourceId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            action = UserAuditActionType.SELF_UNLOCK_ACCOUNT,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}