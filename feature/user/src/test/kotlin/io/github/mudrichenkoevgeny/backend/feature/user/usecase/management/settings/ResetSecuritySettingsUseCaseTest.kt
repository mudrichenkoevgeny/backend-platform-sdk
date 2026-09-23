package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.securitysettings.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.audit.action.SecurityAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.audit.resource.SecurityAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResetSecuritySettingsUseCaseTest {

    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ResetSecuritySettingsUseCase(
        securitySettingsProvider,
        auditLogger,
        auditErrorConverter,
        userManager,
        sessionManager,
        authenticationChallengeService
    )

    private fun mockMfaCheckSuccess(userId: UserId) {
        val userDetails = createTestUserDetails(id = userId)
        val userSession = createTestUserSessionInternal(userId = userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(userSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(userDetails, userSession, true) } returns AppResult.Success(Unit)
    }

    @Test
    fun `successfully resets settings and logs audit`() = runTest {
        val defaultSettings = createTestManagementSecuritySettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)

        mockMfaCheckSuccess(userId)

        coEvery { securitySettingsProvider.resetManagementSecuritySettings() } returns AppResult.Success(defaultSettings)

        val result = useCase(context)

        assertEquals(AppResult.Success(defaultSettings), result)

        coVerify(exactly = 1) {
            securitySettingsProvider.resetManagementSecuritySettings()
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SecurityAuditActionType.MANAGEMENT_RESET_SECURITY_SETTINGS,
                resource = SecurityAuditResourceType.SECURITY_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `handles error and logs failure audit`() = runTest {
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val error = CommonError.Internal(RuntimeException("Database error"))
        val errorLogData = AuditErrorLogData(AuditStatus.FAILED, emptySet())

        mockMfaCheckSuccess(userId)

        coEvery { securitySettingsProvider.resetManagementSecuritySettings() } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SecurityAuditActionType.MANAGEMENT_RESET_SECURITY_SETTINGS,
                resource = SecurityAuditResourceType.SECURITY_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}
