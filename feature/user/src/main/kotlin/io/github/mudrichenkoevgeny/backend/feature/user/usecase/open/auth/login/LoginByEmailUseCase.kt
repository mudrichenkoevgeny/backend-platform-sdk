package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
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
class LoginByEmailUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val authManager: AuthManager
) {
    /**
     * Authenticates an existing user using email and password credentials.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Protects against credential stuffing and brute-force attacks via [UserRateLimitAction.LOGIN_ATTEMPT].
     * - If multifactor authentication (MFA) is enabled for the account, the [authManager] will return
     *   an error directing the user to complete the TOTP challenge.
     *
     * **Workflow:**
     * 1. Validates rate limits for the provided [email].
     * 2. Delegates credential verification to [authManager] for [UserAuthProvider.EMAIL].
     * 3. Logs the security event via [AuditLogger] with [UserAuditActionType.LOGIN_BY_EMAIL].
     *
     * @param email The user's email address.
     * @param password The plaintext password provided for verification.
     * @param requestContext The context of the public request.
     * @return [AppResult] containing [AuthData] or an MFA challenge.
     */
    suspend operator fun invoke(
        email: String,
        password: String,
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
            action = UserRateLimitAction.LOGIN_ATTEMPT,
            identifier = email
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                baseMetadata = auditMetadata
            )
        }

        val authenticateUserResult = authManager.authenticateExistingUser(
            clientInfo = requestContext.clientInfo,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email,
            password = password
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
            action = UserAuditActionType.LOGIN_BY_EMAIL,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}