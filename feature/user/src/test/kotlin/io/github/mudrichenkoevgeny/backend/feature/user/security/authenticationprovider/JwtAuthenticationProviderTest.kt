package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdFromPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.ktor.server.application.ApplicationCall
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class JwtAuthenticationProviderTest {

    private val securityConfig = mockk<SecurityConfig>()
    private val userConfig = mockk<UserConfig>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val appErrorParser = mockk<AppErrorParser>()
    private val call = mockk<ApplicationCall>()

    private lateinit var provider: JwtAuthenticationProvider

    @BeforeEach
    fun setUp() {
        provider = JwtAuthenticationProvider(
            securityConfig,
            userConfig,
            userManager,
            sessionManager,
            appErrorParser
        )
        mockkStatic("io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.JwtExtensionsKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.JwtExtensionsKt")
    }

    @Test
    fun `requireUser should return Success when user has correct role and status`() = runTest {
        val userId = UserId.generate()
        val userDetails = createFakeUser(userId, UserRole.USER, UserAccountStatus.ACTIVE)

        every { call.getUserIdFromPayload() } returns AppResult.Success(userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = provider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.USER),
            allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE),
            requiredPermissions = emptySet()
        )

        assertTrue(result is AppResult.Success)
        assertEquals(userDetails, (result as AppResult.Success).data)
    }

    @Test
    fun `requireUser should return Error when user role is not allowed`() = runTest {
        val userId = UserId.generate()
        val userDetails = createFakeUser(userId, UserRole.USER, UserAccountStatus.ACTIVE)

        every { call.getUserIdFromPayload() } returns AppResult.Success(userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = provider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.ADMIN),
            allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE),
            requiredPermissions = emptySet()
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserRoleNotAllowed)
    }

    @Test
    fun `requireUser should return Error when permissions are missing`() = runTest {
        val userId = UserId.generate()
        val permission = PermissionCode("test.permission")
        val userDetails = createFakeUser(
            userId = userId,
            role = UserRole.USER,
            status = UserAccountStatus.ACTIVE,
            permissions = emptySet()
        )

        every { call.getUserIdFromPayload() } returns AppResult.Success(userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = provider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.USER),
            allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE),
            requiredPermissions = setOf(permission)
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserMissingPermissions)
    }

    @Test
    fun `requireUser should return Error when account status is BANNED`() = runTest {
        val userId = UserId.generate()
        val userDetails = createFakeUser(userId, UserRole.USER, UserAccountStatus.BANNED)

        every { call.getUserIdFromPayload() } returns AppResult.Success(userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = provider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.USER),
            allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE),
            requiredPermissions = emptySet()
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserBlocked)
    }

    @Test
    fun `requireUser should return Error when user is not found`() = runTest {
        val userId = UserId.generate()

        every { call.getUserIdFromPayload() } returns AppResult.Success(userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(null)

        val result = provider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.USER),
            allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE),
            requiredPermissions = emptySet()
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserNotFound)
    }

    private fun createFakeUser(
        userId: UserId,
        role: UserRole,
        status: UserAccountStatus,
        permissions: Set<PermissionCode> = emptySet()
    ): UserDetails {
        val now = Clock.System.now()
        return UserDetails(
            id = userId,
            role = role,
            accountStatus = status,
            accountStatusBeforeDeletion = null,
            authorityLevel = 1,
            permissionCodes = permissions,
            isTotpEnabled = false,
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now,
            updatedAt = now,
            scheduledPermanentDeletionAt = null
        )
    }
}