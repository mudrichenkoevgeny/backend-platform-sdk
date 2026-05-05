package io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeData
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class AuthenticationChallengeServiceImplTest {

    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val mfaService = mockk<MfaService>()
    private val service = AuthenticationChallengeServiceImpl(securitySettingsProvider, mfaService)

    private val testMfaToken = "test-token"
    private val validityUser = 60
    private val validityManager = 300

    @BeforeEach
    fun setup() {
        coEvery { securitySettingsProvider.getRecentAuthenticationValidityInSeconds() } returns validityUser
        coEvery { securitySettingsProvider.getRecentAuthenticationValidityInSecondsForManagement() } returns validityManager
    }

    @Test
    fun `should return success when user session is within validity window`() = runBlocking {
        val userDetails = createUserDetails(UserRole.USER)
        val session = createUserSession(lastReauthenticatedAt = Clock.System.now() - 10.seconds)

        val result = service.ensureSessionConfirmed(userDetails, session)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `should create challenge with correct data when session is expired`() = runBlocking {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val identifierId = UserIdentifierId.generate()

        val userDetails = createUserDetails(UserRole.USER, userId)
        val session = createUserSession(
            lastReauthenticatedAt = Clock.System.now() - 2.hours,
            userId = userId,
            sessionId = sessionId,
            identifierId = identifierId
        )

        val expectedChallengeData = MfaChallengeData(
            token = testMfaToken,
            userId = userId.asHexDashString(),
            userRole = UserRole.USER.serialName,
            identifierId = identifierId.asHexDashString(),
            sessionId = sessionId.asHexDashString(),
            type = MfaChallengeType.STEP_UP
        )

        coEvery {
            mfaService.createChallenge(any(), any(), MfaChallengeType.STEP_UP, any(), any())
        } returns AppResult.Success(expectedChallengeData)

        val result = service.ensureSessionConfirmed(userDetails, session)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is SecurityError.TotpConfirmationRequired)
        assertEquals(testMfaToken, (error as SecurityError.TotpConfirmationRequired).publicArgs?.get("mfaToken"))
    }

    @Test
    fun `should forward error when mfa service fails`() = runBlocking {
        val userDetails = createUserDetails(UserRole.USER)
        val session = createUserSession(lastReauthenticatedAt = null)
        val expectedError = SecurityError.InvalidMfaToken()

        coEvery {
            mfaService.createChallenge(any(), any(), any(), any(), any())
        } returns AppResult.Error(expectedError)

        val result = service.ensureSessionConfirmed(userDetails, session)

        assertTrue(result is AppResult.Error)
        assertEquals(expectedError, (result as AppResult.Error).error)
    }

    private fun createUserDetails(role: UserRole, id: UserId = UserId.generate()) = UserDetails(
        id = id,
        role = role,
        accountStatus = UserAccountStatus.ACTIVE,
        accountStatusBeforeDeletion = null,
        authorityLevel = 1,
        permissionCodes = emptySet(),
        isTotpEnabled = true,
        createdAt = Clock.System.now()
    )

    private fun createUserSession(
        lastReauthenticatedAt: kotlin.time.Instant?,
        userId: UserId = UserId.generate(),
        sessionId: UserSessionId = UserSessionId.generate(),
        identifierId: UserIdentifierId = UserIdentifierId.generate()
    ) = UserSessionInternal(
        id = sessionId,
        userId = userId,
        userRole = UserRole.USER,
        identifier = "test-identifier",
        identifierId = identifierId,
        identifierAuthProvider = UserAuthProvider.EMAIL,
        refreshTokenHash = RefreshTokenHash("hash"),
        deviceInfo = ClientDeviceInfo(),
        userAgent = null,
        ipAddress = null,
        expiresAt = Clock.System.now() + 1.hours,
        lastAccessedAt = Clock.System.now(),
        lastReauthenticatedAt = lastReauthenticatedAt ?: (Clock.System.now() - 1000.hours),
        createdAt = Clock.System.now(),
        updatedAt = null
    )
}