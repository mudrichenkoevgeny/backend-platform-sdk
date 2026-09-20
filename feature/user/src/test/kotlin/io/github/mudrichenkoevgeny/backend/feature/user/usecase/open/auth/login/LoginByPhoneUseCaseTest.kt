package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginByPhoneUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val otpService = mockk<OtpService>()
    private val authManager = mockk<AuthManager>()
    private val lockoutManager = mockk<LockoutManager>(relaxed = true)
    private val identifierManager = mockk<IdentifierManager>()
    private val userManager = mockk<UserManager>()

    private val useCase = LoginByPhoneUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        otpService = otpService,
        authManager = authManager,
        lockoutManager = lockoutManager,
        identifierManager = identifierManager,
        userManager = userManager
    )

    @BeforeEach
    fun setup() {
        coEvery { lockoutManager.isIndefiniteLockout(any()) } returns AppResult.Success(false)
        coEvery { lockoutManager.getLockoutUntil(any()) } returns AppResult.Success(null)
        coEvery { lockoutManager.clearLockout(any()) } returns AppResult.Success(Unit)
        coEvery { lockoutManager.recordFailedAttempt(any(), any()) } returns AppResult.Success(null)
    }

    @Test
    fun `successfully authenticates by phone and logs audit`() = runTest {
        val context = createTestRequestContext()
        val userId = UserId.generate()
        val userDetails = mockk<UserDetails> {
            every { id } returns userId
            every { role } returns UserRole.USER
        }
        val authData = mockk<AuthData> {
            every { this@mockk.userDetails } returns userDetails
        }

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_PHONE)
        } returns AppResult.Success(Unit)

        coEvery {
            otpService.verifyOtp(TEST_PHONE, UserOtpVerificationType.PHONE_VERIFICATION, TEST_CODE)
        } returns AppResult.Success(true)

        coEvery {
            authManager.authenticateOrCreateUser(
                clientInfo = context.clientInfo,
                userAuthProvider = UserAuthProvider.PHONE,
                identifier = TEST_PHONE,
                password = null,
                allowedRoles = any(),
                allowedAccountStatuses = any()
            )
        } returns AppResult.Success(authData)

        val result = useCase(TEST_PHONE, TEST_CODE, context)

        assertEquals(AppResult.Success(authData), result)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.USER.serialName,
                action = UserAuditActionType.LOGIN_BY_PHONE,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = match { meta -> meta.any { it.value == TEST_PHONE } },
                message = null,
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createTestRequestContext()
        val error = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_PHONE)
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(TEST_PHONE, TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = null,
                actorType = AuditActorType.USER,
                actorUserRole = null,
                action = UserAuditActionType.LOGIN_BY_PHONE,
                resource = UserAuditResourceType.USER,
                resourceId = null,
                status = AuditStatus.FAILED,
                metadata = any(),
                message = null
            )
        }
    }

    @Test
    fun `returns error when OTP code is incorrect`() = runTest {
        val context = createTestRequestContext()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { otpService.verifyOtp(any(), any(), any()) } returns AppResult.Success(false)
        every { auditErrorConverter.convert(any<UserError.WrongConfirmationCode>()) } returns errorLogData

        val result = useCase(TEST_PHONE, TEST_CODE, context)

        assertTrue(result is AppResult.Error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorType = AuditActorType.USER,
                action = UserAuditActionType.LOGIN_BY_PHONE,
                resource = UserAuditResourceType.USER,
                status = AuditStatus.FAILED,
                metadata = any(),
                message = null
            )
        }
    }

    @Test
    fun `returns error when auth manager fails`() = runTest {
        val context = createTestRequestContext()
        val authError = UserError.UserBlocked()
        val errorLogData = AuditErrorLogData(status = AuditStatus.DENIED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { otpService.verifyOtp(any(), any(), any()) } returns AppResult.Success(true)
        coEvery {
            authManager.authenticateOrCreateUser(
                clientInfo = any(),
                userAuthProvider = any(),
                identifier = any(),
                password = any(),
                externalProviderEmail = any(),
                roleForUserCreation = any(),
                accountStatusForUserCreation = any(),
                authorityLevelForUserCreation = any(),
                permissionCodesForUserCreation = any(),
                allowedRoles = any(),
                allowedAccountStatuses = any()
            )
        } returns AppResult.Error(authError)

        every { auditErrorConverter.convert(authError) } returns errorLogData

        val result = useCase(TEST_PHONE, TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorType = AuditActorType.USER,
                action = UserAuditActionType.LOGIN_BY_PHONE,
                resource = UserAuditResourceType.USER,
                status = AuditStatus.DENIED,
                metadata = any(),
                message = null
            )
        }
    }

    companion object {
        private const val TEST_PHONE = "+79991234567"
        private const val TEST_CODE = "123456"
    }
}
