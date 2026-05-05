package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementCreateUserUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val authManager = mockk<AuthManager>()
    private val sessionManager = mockk<SessionManager>()
    private val validatePasswordUseCase = mockk<ValidatePasswordUseCase>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ManagementCreateUserUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        authManager = authManager,
        sessionManager = sessionManager,
        validatePasswordUseCase = validatePasswordUseCase,
        authenticationChallengeService = authenticationChallengeService
    )

    private val managerId = UserId.generate()
    private val sessionId = UserSessionId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = managerId,
        userRole = UserRole.ADMIN,
        sessionId = sessionId,
        clientInfo = mockk(relaxed = true)
    )

    @Test
    fun `successfully creates user`() = runTest {
        val permission = UserPermissionCode.USER_CREATE_AS_USER
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { permissionCodes } returns setOf(permission)
        }
        val createdUser = mockk<UserDetails> {
            every { id } returns UserId.generate()
        }
        val managerSession = mockk<UserSessionInternal>()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { validatePasswordUseCase(any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(managerSession)
        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(any(), any())
        } returns AppResult.Success(Unit)

        coEvery {
            authManager.createUserAndIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = "test@example.com",
                password = "password123",
                externalProviderEmail = null,
                roleForUserCreation = UserRole.USER,
                accountStatusForUserCreation = UserAccountStatus.ACTIVE,
                authorityLevelForUserCreation = 50,
                permissionCodesForUserCreation = setOf(permission)
            )
        } returns AppResult.Success(createdUser)

        val result = useCase(
            email = "test@example.com",
            password = "password123",
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50,
            permissionCodes = setOf(permission),
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(createdUser), result)
        coVerify {
            authManager.createUserAndIdentifier(
                userAuthProvider = any(),
                identifier = any(),
                password = any(),
                externalProviderEmail = any(),
                roleForUserCreation = any(),
                accountStatusForUserCreation = any(),
                authorityLevelForUserCreation = any(),
                permissionCodesForUserCreation = any()
            )
        }
    }

    @Test
    fun `returns error when authority level is too high`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 50
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(
            email = "test@example.com",
            password = "password123",
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 60,
            permissionCodes = emptySet(),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when manager lacks required creation permission`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { permissionCodes } returns emptySet()
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(
            email = "test@example.com",
            password = "password123",
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50,
            permissionCodes = emptySet(),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error when manager attempts to grant permissions they do not have`() = runTest {
        val managerPermission = UserPermissionCode.USER_CREATE_AS_USER
        val extraPermission = UserPermissionCode.USER_UPDATE_SECURITY_FOR_STAFF

        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { permissionCodes } returns setOf(managerPermission)
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(
            email = "test@example.com",
            password = "password123",
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50,
            permissionCodes = setOf(extraPermission),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error when creating non-staff or non-user role`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(
            email = "test@example.com",
            password = "password123",
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50,
            permissionCodes = emptySet(),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}