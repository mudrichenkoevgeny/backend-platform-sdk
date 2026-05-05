package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
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
class LoginByPhoneUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val otpService: OtpService,
    private val authManager: AuthManager
) {
    /**
     * Authenticates or creates a user account using a phone number and OTP verification.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Requires a valid [confirmationCode] previously issued via [OtpService].
     * - Protects against brute-force attempts on the OTP via [UserRateLimitAction.LOGIN_ATTEMPT].
     * - If multifactor authentication (MFA) is enabled for the account, the [authManager] will return
     *   an error directing the user to complete the TOTP challenge.
     *
     * **Workflow:**
     * 1. Validates rate limits for the provided [phoneNumber].
     * 2. Verifies the [confirmationCode] for the [UserOtpVerificationType.PHONE_VERIFICATION] type.
     * 3. Delegates authentication or user creation to [authManager] for [UserAuthProvider.PHONE].
     * 4. Logs the security event via [AuditLogger] with [UserAuditActionType.LOGIN_BY_PHONE].
     *
     * @param phoneNumber The user's phone number in E.164 format.
     * @param confirmationCode The OTP code received by the user via SMS.
     * @param requestContext The context of the public request.
     * @return [AppResult] containing [AuthData] or an MFA challenge.
     */
    suspend operator fun invoke(
        phoneNumber: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditMetadata = requestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.PHONE_NUMBER,
                value = phoneNumber
            )
        )

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
            type = UserOtpVerificationType.PHONE_VERIFICATION,
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

        val authenticateUserResult = authManager.authenticateOrCreateUser(
            clientInfo = requestContext.clientInfo,
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = phoneNumber,
            password = null
        )

        return when (authenticateUserResult) {
            is AppResult.Success -> {
                val userDetails = authenticateUserResult.data.userDetails
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = userDetails.id.asHexDashString(),
                    actorUserRole = userDetails.role,
                    resourceId = userDetails.id.asHexDashString(),
                    metadata = auditMetadata
                )
                authenticateUserResult
            }
            is AppResult.Error -> handleError(
                error = authenticateUserResult.error,
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
            action = UserAuditActionType.LOGIN_BY_PHONE,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}