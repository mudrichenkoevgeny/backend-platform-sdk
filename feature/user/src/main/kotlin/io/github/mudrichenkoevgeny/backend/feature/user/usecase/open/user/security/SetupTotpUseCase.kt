package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessor
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.totpsetup.TotpSetup
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetupTotpUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val identifierManager: IdentifierManager,
    private val totpManager: TotpManager,
    private val mfaService: MfaService,
    private val totpCryptoProcessor: TotpCryptoProcessor
) {
    /**
     * Initiates the setup process for Time-based One-Time Password (TOTP) multifactor authentication.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.USER_SETUP_TOTP].
     * 2. Validates that the user exists and does not already have TOTP enabled.
     * 3. Retrieves the current session and determines the account name for the TOTP issuer.
     * 4. Generates a new TOTP secret and provisioning URI (otpauth://).
     * 5. Persists the encrypted secret as a pending setup via [TotpManager].
     * 6. Creates an MFA challenge of type [MfaChallengeType.SETUP_TOTP] to be verified via [EnableTotpUseCase].
     * 7. Logs the initiation via [AuditLogger] with [UserAuditActionType.SELF_SETUP_TOTP_INITIATED].
     *
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing [TotpSetup] with the secret key, QR code URL, and MFA token.
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<TotpSetup> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()
        val currentUserId = authenticatedRequestContext.userId

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.USER_SETUP_TOTP,
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

        val userSessionResult = sessionManager.getUserSessionForSystem(
            userSessionId = authenticatedRequestContext.sessionId
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = UserSession::class.java.simpleName,
                identifier = authenticatedRequestContext.sessionId.asHexDashString()
            )
        )
        val userSession = when (userSessionResult) {
            is AppResult.Error -> return handleError(
                error = userSessionResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> userSessionResult.data
        }

        val accountNameResult = getAccountName(userSession)
        val accountName = when (accountNameResult) {
            is AppResult.Error -> return handleError(
                error = accountNameResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> accountNameResult.data
        }

        val generatedTotpResult = totpCryptoProcessor.generateNewSecret(
            accountName = accountName
        )

        val generatedTotp = when (generatedTotpResult) {
            is AppResult.Error -> return handleError(
                error = generatedTotpResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> generatedTotpResult.data
        }

        val initiationResult = totpManager.initiateTotpSetup(
            userId = currentUserId,
            encryptedSecret = generatedTotp.encryptedSecret
        )
        if (initiationResult is AppResult.Error) {
            return handleError(
                error = initiationResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val mfaChallengeResult = mfaService.createChallenge(
            userId = auditActorId,
            userRole = user.role.serialName,
            type = MfaChallengeType.SETUP_TOTP,
            identifierId = userSession.identifierId.asHexDashString(),
            sessionId = authenticatedRequestContext.sessionId.asHexDashString()
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

        logAudit(
            actorId = auditActorId,
            actorUserRole = auditActorUserRole,
            status = AuditStatus.SUCCESS,
            metadata = auditMetadata
        )

        return AppResult.Success(
            TotpSetup(
                secretKey = generatedTotp.decryptedSecret.value,
                otpAuthUrl = generatedTotp.otpAuthUrl,
                mfaToken = mfaChallenge.token
            )
        )
    }

    private suspend fun getAccountName(
        userSession: UserSessionInternal
    ): AppResult<String> {
        if (userSession.identifierAuthProvider == UserAuthProvider.EMAIL
            || userSession.identifierAuthProvider == UserAuthProvider.PHONE) {
            return AppResult.Success(userSession.identifier)
        }

        val userIdentifierResult = identifierManager.getUserIdentifierByIdForSystem(
            userIdentifierId = userSession.identifierId
        )
        val externalIdentifierEmail = when (userIdentifierResult) {
            is AppResult.Error -> return userIdentifierResult
            is AppResult.Success -> userIdentifierResult.data?.externalProviderEmail
        }

        val providerName = userSession.identifierAuthProvider.serialName
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        val detail = if (!externalIdentifierEmail.isNullOrBlank()) {
            externalIdentifierEmail
        } else {
            userSession.identifier.take(12)
        }

        return AppResult.Success("$providerName: $detail")
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
            action = UserAuditActionType.SELF_SETUP_TOTP_INITIATED,
            resource = UserAuditResourceType.USER,
            resourceId = actorId,
            status = status,
            metadata = metadata
        )
    }
}