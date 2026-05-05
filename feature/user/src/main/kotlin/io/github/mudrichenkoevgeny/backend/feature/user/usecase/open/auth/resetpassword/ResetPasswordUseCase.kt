package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.resetpassword

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResetPasswordUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val otpService: OtpService,
    private val identifierManager: IdentifierManager,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    /**
     * Resets a user's password using an OTP verification code sent to their email.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Enforces password complexity rules via [ValidatePasswordUseCase].
     * - Requires a valid [confirmationCode] previously issued via [OtpService].
     * - Prevents brute-force attacks via rate limiting on [UserRateLimitAction.PASSWORD_CHANGE].
     *
     * **Workflow:**
     * 1. Checks rate limits for the provided email.
     * 2. Validates the [newPassword] against the system's security policy.
     * 3. Verifies the [confirmationCode] for the [UserOtpVerificationType.EMAIL_PASSWORD_RESET] type.
     * 4. Resolves the user's [UserIdentifier] associated with the provided email.
     * 5. Updates the identifier's password hash via [IdentifierManager].
     * 6. Logs the security event via [AuditLogger] with [UserAuditActionType.RESET_PASSWORD].
     *
     * @param email The email address of the account to reset.
     * @param newPassword The new password to be set.
     * @param confirmationCode The OTP code received by the user.
     * @param requestContext The context of the public request.
     * @return [AppResult.Success] if the password was updated, or an [AppResult.Error] otherwise.
     */
    suspend operator fun invoke(
        email: String,
        newPassword: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<Unit> {
        val auditMetadata = requestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.EMAIL_ADDRESS,
                value = email
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.PASSWORD_CHANGE,
            identifier = email
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                baseMetadata = auditMetadata
            )
        }

        val passwordPolicyCheckResult = validatePasswordUseCase(newPassword)
        if (passwordPolicyCheckResult is AppResult.Error) {
            return handleError(
                error = passwordPolicyCheckResult.error,
                baseMetadata = auditMetadata
            )
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = email,
            type = UserOtpVerificationType.EMAIL_PASSWORD_RESET,
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
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
        ).mapNotNullOrError(UserError.UserNotFound())

        val userIdentifier = when (identifierResult) {
            is AppResult.Success -> identifierResult.data
            is AppResult.Error -> return handleError(
                error = identifierResult.error,
                baseMetadata = auditMetadata
            )
        }

        val updateUserIdentifierResult = identifierManager.updateUserIdentifierPassword(
            userIdentifier = userIdentifier,
            password = newPassword
        )

        return when (updateUserIdentifierResult) {
            is AppResult.Error -> handleError(
                error = updateUserIdentifierResult.error,
                actorId = userIdentifier.userId.asHexDashString(),
                resourceId = userIdentifier.userId.asHexDashString(),
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> {
                logAudit(
                    actorId = userIdentifier.userId.asHexDashString(),
                    resourceId = userIdentifier.userId.asHexDashString(),
                    status = AuditStatus.SUCCESS,
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
            action = UserAuditActionType.RESET_PASSWORD,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}