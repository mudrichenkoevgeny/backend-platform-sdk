package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.register

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegisterByEmailUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val otpService: OtpService,
    private val authManager: AuthManager,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    /**
     * Completes the user registration process using an email address and OTP verification.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Enforces password complexity rules via [ValidatePasswordUseCase].
     * - Requires a valid [confirmationCode] previously issued via [OtpService].
     * - Protects against registration floods via [UserRateLimitAction.REGISTRATION_ATTEMPT].
     *
     * **Workflow:**
     * 1. Checks rate limits for the provided email.
     * 2. Validates the [password] against the system's security policy.
     * 3. Verifies the [confirmationCode] for the [UserOtpVerificationType.EMAIL_VERIFICATION] type.
     * 4. Delegates user creation and initial authentication to [AuthManager].
     * 5. Logs the security event via [AuditLogger] with [UserAuditActionType.REGISTER_BY_EMAIL].
     *
     * @param email The email address being registered.
     * @param password The password for the new account.
     * @param confirmationCode The OTP code received by the user.
     * @param requestContext The context of the public request.
     * @return [AppResult] containing [AuthData] (tokens and user details) upon successful registration.
     */
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditMetadata = requestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.EMAIL_ADDRESS,
                value = email
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.REGISTRATION_ATTEMPT,
            identifier = email
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                baseMetadata = auditMetadata
            )
        }

        val passwordPolicyCheckResult = validatePasswordUseCase(password)
        if (passwordPolicyCheckResult is AppResult.Error) {
            return handleError(
                error = passwordPolicyCheckResult.error,
                baseMetadata = auditMetadata
            )
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = email,
            type = UserOtpVerificationType.EMAIL_VERIFICATION,
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

        val registrationResult = authManager.authenticateOrCreateUser(
            clientInfo = requestContext.clientInfo,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email,
            password = password
        )

        return when (registrationResult) {
            is AppResult.Success -> {
                val userDetails = registrationResult.data.userDetails
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = userDetails.id.asHexDashString(),
                    actorUserRole = userDetails.role,
                    resourceId = userDetails.id.asHexDashString(),
                    metadata = auditMetadata
                )
                registrationResult
            }
            is AppResult.Error -> handleError(
                error = registrationResult.error,
                baseMetadata = auditMetadata
            )
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        resourceId: String? = null,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            actorUserRole = actorUserRole,
            resourceId = resourceId,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
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
            action = UserAuditActionType.REGISTER_BY_EMAIL,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}