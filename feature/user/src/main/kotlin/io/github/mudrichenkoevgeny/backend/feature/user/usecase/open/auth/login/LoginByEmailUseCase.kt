package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

// todo refactor
/**
 * Use case: authenticate a user by email and password.
 *
 * Applies rate limiting, resolves the email identifier, validates password via [PasswordHasher],
 * then provides auth data (tokens, user) via [AuthManager]. Audit is logged for success, wrong password, or unregistered email.
 * [execute] takes email, password, and request context;
 * returns [AppResult.Success] with [AuthData] or [AppResult.Error] (e.g. [UserError.InvalidCredentials], rate limit).
 */
@Singleton
class LoginByEmailUseCase @Inject constructor(
    private val passwordHasher: PasswordHasher,
    private val identifierManager: IdentifierManager,
    private val authManager: AuthManager,
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger
) {
    suspend fun execute(
        email: String,
        password: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val metadata: Set<AuditEventMetadata> = requestContext.clientInfo.toAuditMetadata()

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.LOGIN_ATTEMPT,
            identifier = email
        )

        if (rateLimitCheck is AppResult.Error) {
            // todo audit log?
            return rateLimitCheck
        }

        val userIdentifierResult = identifierManager.getUserIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
        )

        val userIdentifier = when (userIdentifierResult) {
            is AppResult.Success -> userIdentifierResult.data
            is AppResult.Error -> {
                // todo audit log?
                return userIdentifierResult
            }
        }

        if (userIdentifier == null) {
            passwordHasher.isPasswordValidFakeCheck(password)
            // todo audit log?
            return AppResult.Error(UserError.InvalidCredentials())
        }

        val isPasswordValidResult = passwordHasher.isPasswordValid(password, userIdentifier.passwordHash)

        val isPasswordValid = when (isPasswordValidResult) {
            is AppResult.Success -> isPasswordValidResult.data
            is AppResult.Error -> {
                // todo audit log?
                return isPasswordValidResult
            }
        }

        if (!isPasswordValid) {
            // todo audit log?
            return AppResult.Error(UserError.InvalidCredentials())
        }

        val authDataResult = authManager.provideAuthData(
            userIdentifier = userIdentifier,
            clientInfo = requestContext.clientInfo,
            allowedRoles = setOf(UserRole.USER)
        )

        when (authDataResult) {
            is AppResult.Success -> {
                logAudit(
                    actorId = authDataResult.data.currentUser
                )
            }
            is AppResult.Error -> {

            }

        }

        return authDataResult
    }

    private fun logAudit(
        actorId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = requestContext.userId?.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = null,
            action = UserAuditActionType.LOGIN_BY_EMAIL,
            resource = UserAuditResourceType.USER_EMAIL,
            status = status,
            metadata = metadata
        )
    }
}