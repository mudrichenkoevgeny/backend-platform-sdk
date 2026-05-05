package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddUserIdentifierEmailUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val authManager: AuthManager,
    private val otpService: OtpService,
    private val authenticationChallengeService: AuthenticationChallengeService,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    /**
     * Links a new email identifier to an existing user account after OTP verification and password validation.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Security:**
     * - Sensitive operation requiring MFA Step-up (session confirmation).
     * - Enforces password complexity rules via [ValidatePasswordUseCase].
     * - Requires valid OTP verification for the target email address.
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.USER_IDENTIFIER_ADD].
     * 2. Ensures the current session is recently confirmed via [AuthenticationChallengeService].
     * 3. Validates the [password] against the system's password policy.
     * 4. Verifies the [confirmationCode] against the [email] via [OtpService].
     * 5. Creates a new [UserAuthProvider.EMAIL] identifier record via [AuthManager].
     * 6. Logs the security event via [AuditLogger] with [UserAuditActionType.ADD_IDENTIFIER_EMAIL].
     *
     * @param email The email address to be linked.
     * @param password The password to be associated with this identifier.
     * @param confirmationCode The OTP code received by the user via email.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing the newly created [UserIdentifier].
     */
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmationCode: String,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserIdentifier> {
        val currentUserId = authenticatedRequestContext.userId
        val currentSessionId = authenticatedRequestContext.sessionId

        val auditActorId = currentUserId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()

        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.SESSION_ID,
                value = currentSessionId.asHexDashString()
            )
        )
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.EMAIL_ADDRESS,
                value = email
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.USER_IDENTIFIER_ADD,
            identifier = currentUserId.asHexDashString()
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

        val passwordPolicyCheckResult = validatePasswordUseCase(password)
        if (passwordPolicyCheckResult is AppResult.Error) {
            return handleError(
                error = passwordPolicyCheckResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = email,
            type = UserOtpVerificationType.EMAIL_VERIFICATION,
            code = confirmationCode
        )
        when (verifyOtpResult) {
            is AppResult.Error -> return handleError(
                error = verifyOtpResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> if (!verifyOtpResult.data) {
                return handleError(
                    error = UserError.WrongConfirmationCode(),
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    baseMetadata = auditMetadata
                )
            }
        }

        val createIdentifierResult = authManager.createIdentifierForAuthorizedUser(
            userId = currentUserId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email,
            password = password,
            externalProviderEmail = null
        )

        when (createIdentifierResult) {
            is AppResult.Error -> {
                return handleError(
                    error = createIdentifierResult.error,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    baseMetadata = auditMetadata
                )
            }
            is AppResult.Success -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = createIdentifierResult.data.id.asHexDashString(),
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
        }

        return createIdentifierResult
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole,
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
        actorId: String,
        actorUserRole: UserRole,
        resourceId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole.serialName,
            action = UserAuditActionType.ADD_IDENTIFIER_EMAIL,
            resource = UserAuditResourceType.IDENTIFIER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}