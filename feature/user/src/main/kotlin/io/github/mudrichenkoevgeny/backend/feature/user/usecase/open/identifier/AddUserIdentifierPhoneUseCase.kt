package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.resource.UserAuditResourceType
import javax.inject.Inject
import javax.inject.Singleton

// todo refactor
/**
 * Use case: add a phone number as a new authentication identifier for the current user.
 *
 * Requires recent authentication confirmation. Ensures the user does not already have a phone identifier and that the phone is not used by another account.
 * Verifies OTP, then creates the identifier via [AuthManager.getOrCreateUserIdentifier].
 * [execute] takes phoneNumber, confirmationCode, and request context;
 * returns [AppResult.Success] with new [UserIdentifier] or [AppResult.Error] (e.g. [UserError.CannotCreateUserIdentifier], [UserError.WrongConfirmationCode]).
 */
@Singleton
class AddUserIdentifierPhoneUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val sessionManager: SessionManager,
    private val identifierManager: IdentifierManager,
    private val authManager: AuthManager,
    private val authenticationPolicyChecker: AuthenticationPolicyChecker
) {
    suspend fun execute(
        phoneNumber: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<UserIdentifier> {
        val userId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val currentSessionId = requestContext.sessionId
            ?: return AppResult.Error(UserError.InvalidSession())

        val auditResourceId = userId.asHexDashString()

        val auditMetadata = mutableMapOf(
            UserAuditMetadata.Keys.PHONE_NUMBER_MASK to DataMasker.maskPhone(phoneNumber),
            UserAuditMetadata.Keys.SESSION_ID to currentSessionId.asHexDashString()
        )

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = UserRateLimitAction.USER_IDENTIFIER_CHANGE,
            rateLimitIdentifier = phoneNumber,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val userSessionResult = sessionManager.getUserSessionById(currentSessionId)

        val currentSession = when (userSessionResult) {
            is AppResult.Success -> userSessionResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return userSessionResult
            }
        }

        if (currentSession == null) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
            )
            return AppResult.Error(UserError.CannotCreateUserIdentifier())
        }

        val isAuthenticationConfirmedRecently = authenticationPolicyChecker.isAuthenticationConfirmedRecently(
            lastReauthenticatedAt = currentSession.lastReauthenticatedAt
        )

        if (!isAuthenticationConfirmedRecently) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.AUTHENTICATION_CONFIRMATION_REQUIRED,
                metadata = auditMetadata
            )
            return AppResult.Error(SecurityError.AuthenticationConfirmationRequired())
        }

        val userIdentifiersListResult = identifierManager.getUserIdentifiersByUserId(userId)

        val userIdentifiersList = when (userIdentifiersListResult) {
            is AppResult.Success -> userIdentifiersListResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return userIdentifiersListResult
            }
        }

        val existingUserIdentifierPhone = userIdentifiersList.find { userIdentifier ->
            userIdentifier.userAuthProvider == UserAuthProvider.PHONE
        }

        if (existingUserIdentifierPhone != null) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.ALREADY_HAS_USER_IDENTIFIER_WITH_THAT_TYPE,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.AlreadyHasUserIdentifierWithThatType())
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = phoneNumber,
            type = OtpVerificationType.PHONE_VERIFICATION,
            code = confirmationCode
        )

        val isConfirmationCodeCorrect = when (verifyOtpResult) {
            is AppResult.Success -> verifyOtpResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return verifyOtpResult
            }
        }

        if (!isConfirmationCodeCorrect) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.WRONG_VERIFICATION_CODE,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.WrongConfirmationCode())
        }

        val getUserIdentifierResult = identifierManager.getUserIdentifier(
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = phoneNumber
        )

        val existingUserIdentifier = when (getUserIdentifierResult) {
            is AppResult.Success -> getUserIdentifierResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return getUserIdentifierResult
            }
        }

        if (existingUserIdentifier != null) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.PHONE_ALREADY_REGISTERED,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.CannotCreateUserIdentifier())
        }

        val userIdentifierResult = authManager.getOrCreateUserIdentifier(
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = phoneNumber,
            userRole = UserRole.USER
        )

        return when (userIdentifierResult) {
            is AppResult.Success -> {
                auditMetadata[UserAuditMetadata.Keys.USER_IDENTIFIER_ID] = userIdentifierResult.data.id.asHexDashString()
                userAuditLogger.logSuccess(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId,
                    metadata = auditMetadata
                )
                userIdentifierResult
            }
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return userIdentifierResult
            }
        }
    }

    private fun logAuditInternalError(
        requestContext: RequestContext,
        auditResourceId: String?,
        auditMetadata: Map<String, String>
    ) {
        userAuditLogger.logInternalError(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            metadata = auditMetadata
        )
    }

    companion object {
        const val AUDIT_ACTION = UserAuditActionType.ACTION_ADD_IDENTIFIER_PHONE
        const val AUDIT_RESOURCE = UserAuditResourceType.RESOURCE_USER
    }
}