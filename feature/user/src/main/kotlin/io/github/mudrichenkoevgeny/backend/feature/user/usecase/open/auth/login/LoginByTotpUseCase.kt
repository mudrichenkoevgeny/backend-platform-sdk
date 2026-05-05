package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginByTotpUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val mfaService: MfaService,
    private val totpManager: TotpManager,
    private val authManager: AuthManager
) {
    /**
     * Completes the multifactor authentication flow using a TOTP (Time-based One-Time Password).
     *
     * **Allowed Account Statuses:** Any (Handled by authentication logic).
     *
     * **Security:**
     * - Requires a valid [mfaToken] issued during the initial login step.
     * - Protects against brute-force attempts on TOTP code via [UserRateLimitAction.LOGIN_ATTEMPT].
     * - Consumes the MFA challenge upon successful verification to prevent replay attacks.
     *
     * **Workflow:**
     * 1. Validates the rate limit for the provided [mfaToken].
     * 2. Retrieves the MFA challenge context (User ID, Role, Identifier ID) from [MfaService].
     * 3. Verifies the provided [code] against the user's stored TOTP secret via [TotpManager].
     * 4. If valid, consumes the challenge and completes the authentication session via [AuthManager].
     * 5. Logs the security event via [AuditLogger] with [UserAuditActionType.LOGIN_BY_TOTP].
     *
     * @param requestContext The context of the public request.
     * @param mfaToken The temporary token representing the ongoing MFA session.
     * @param code Code from the user's authenticator app.
     * @return [AppResult] containing [AuthData] upon successful authentication.
     */
    suspend operator fun invoke(
        requestContext: RequestContext,
        mfaToken: String,
        code: String
    ): AppResult<AuthData> {
        val auditMetadata = requestContext.clientInfo.toAuditMetadata()

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.LOGIN_ATTEMPT,
            identifier = mfaToken
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                baseMetadata = auditMetadata
            )
        }

        val getChallengeResult = mfaService.getChallenge(
            token = mfaToken,
            type = MfaChallengeType.LOGIN_TOTP
        )
        val mfaChallengeData = when (getChallengeResult) {
            is AppResult.Error -> return handleError(
                error = getChallengeResult.error,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> getChallengeResult.data
        }

        val userId = mfaChallengeData.userId.toUserIdOrNull()
        val userRole = UserRole.fromValueOrNull(mfaChallengeData.userRole)
        val identifierId = mfaChallengeData.identifierId?.toUserIdentifierIdOrNull()
        if (userId == null || identifierId == null) {
            val error = SecurityError.InvalidMfaToken()
            return handleError(
                error = error,
                baseMetadata = auditMetadata
            )
        }

        val verifyResult = totpManager.verifyTotp(
            userId = userId,
            code = code
        )
        if (verifyResult is AppResult.Error) {
            return handleError(
                error = verifyResult.error,
                actorId = userId.asHexDashString(),
                actorUserRole = userRole,
                baseMetadata = auditMetadata
            )
        }

        mfaService.consumeChallenge(mfaToken)

        val mfaAuthenticationResult = authManager.completeMfaAuthentication(
            userId = userId,
            userIdentifierId = identifierId,
            clientInfo = requestContext.clientInfo
        )

        return when (mfaAuthenticationResult) {
            is AppResult.Success -> {
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = mfaAuthenticationResult.data.userDetails.id.asHexDashString(),
                    actorUserRole = mfaAuthenticationResult.data.userDetails.role,
                    metadata = auditMetadata
                )
                mfaAuthenticationResult
            }
            is AppResult.Error -> handleError(
                error = mfaAuthenticationResult.error,
                actorId = userId.asHexDashString(),
                actorUserRole = userRole,
                baseMetadata = auditMetadata
            )
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String? = null,
        actorUserRole: UserRole? = null,
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
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.LOGIN_BY_TOTP,
            resource = UserAuditResourceType.USER,
            resourceId = actorId,
            status = status,
            metadata = metadata
        )
    }
}