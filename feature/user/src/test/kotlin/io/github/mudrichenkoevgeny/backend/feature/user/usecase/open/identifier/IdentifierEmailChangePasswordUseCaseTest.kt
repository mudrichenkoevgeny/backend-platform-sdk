package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IdentifierEmailChangePasswordUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val identifierManager = mockk<IdentifierManager>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()
    private val validatePasswordUseCase = mockk<ValidatePasswordUseCase>()

    private val useCase = IdentifierEmailChangePasswordUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        identifierManager = identifierManager,
        passwordHasher = passwordHasher,
        authenticationChallengeService = authenticationChallengeService,
        validatePasswordUseCase = validatePasswordUseCase
    )

    private fun createAuthContext() = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully changes password`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val sessionInternal = mockk<UserSessionInternal>()
        val identifierId = UserIdentifierId.generate()
        val identifierInternal = mockk<UserIdentifierInternal> {
            every { id } returns identifierId
            every { passwordHash } returns PasswordHash("old_hash")
        }
        val updatedIdentifier = mockk<UserIdentifier>()

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.PASSWORD_CHANGE, TEST_EMAIL)
        } returns AppResult.Success(Unit)

        coEvery { userManager.getUserByIdForSelf(context.userId) } returns AppResult.Success(userDetails)

        coEvery { sessionManager.getUserSessionForSystem(context.sessionId) } returns AppResult.Success(sessionInternal)

        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(userDetails, sessionInternal)
        } returns AppResult.Success(Unit)

        coEvery { validatePasswordUseCase(TEST_NEW_PASS) } returns AppResult.Success(Unit)

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL)
        } returns AppResult.Success(identifierInternal)

        coEvery {
            passwordHasher.isPasswordValid(TEST_OLD_PASS, PasswordHash("old_hash"))
        } returns AppResult.Success(true)

        coEvery {
            identifierManager.updateUserIdentifierPassword(identifierInternal, TEST_NEW_PASS)
        } returns AppResult.Success(updatedIdentifier)

        val result = useCase(TEST_EMAIL, TEST_NEW_PASS, TEST_OLD_PASS, context)

        assertEquals(AppResult.Success(updatedIdentifier), result)

        coVerify {
            auditLogger.log(
                action = UserAuditActionType.CHANGE_PASSWORD,
                status = AuditStatus.SUCCESS,
                actorId = context.userId.asHexDashString(),
                resourceId = identifierId.asHexDashString(),
                resource = UserAuditResourceType.IDENTIFIER,
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when old password is wrong`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val sessionInternal = mockk<UserSessionInternal>()
        val identifierId = UserIdentifierId.generate()
        val identifierInternal = mockk<UserIdentifierInternal> {
            every { id } returns identifierId
            every { passwordHash } returns PasswordHash("old_hash")
        }
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)

        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(userDetails)

        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(sessionInternal)

        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)

        coEvery { validatePasswordUseCase(any()) } returns AppResult.Success(Unit)

        coEvery { identifierManager.getUserIdentifierInternalByProvider(any(), any()) } returns AppResult.Success(identifierInternal)

        coEvery { passwordHasher.isPasswordValid(any(), any()) } returns AppResult.Success(false)

        every { auditErrorConverter.convert(any<UserError.WrongPassword>()) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_NEW_PASS, "wrong_pass", context)

        assertTrue(result is AppResult.Error && result.error is UserError.WrongPassword)

        coVerify {
            auditLogger.log(
                status = AuditStatus.FAILED,
                action = UserAuditActionType.CHANGE_PASSWORD,
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = identifierId.asHexDashString(),
                actorUserRole = context.userRole.serialName,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when password policy validation fails`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val sessionInternal = mockk<UserSessionInternal>()
        val policyError = mockk<SecurityError.PasswordTooWeak>()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)

        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(userDetails)

        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(sessionInternal)

        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)

        coEvery { validatePasswordUseCase("123") } returns AppResult.Error(policyError)

        every { auditErrorConverter.convert(policyError) } returns errorLogData

        val result = useCase(TEST_EMAIL, "123", TEST_OLD_PASS, context)

        assertTrue(result is AppResult.Error && result.error == policyError)
    }

    companion object {
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_OLD_PASS = "OldPass123!"
        private const val TEST_NEW_PASS = "NewPass456!"
    }
}