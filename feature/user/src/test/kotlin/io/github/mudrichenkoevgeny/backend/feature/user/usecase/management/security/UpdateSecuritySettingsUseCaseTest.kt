package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.security.domain.model.securitysettings.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.security.domain.model.securitysettings.createTestOpenSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.contract.SecurityWebSocketEventTypes
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

class UpdateSecuritySettingsUseCaseTest {

    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = UpdateSecuritySettingsUseCase(
        securitySettingsProvider,
        auditLogger,
        auditErrorConverter,
        webSocketManager,
        userManager,
        sessionManager,
        authenticationChallengeService
    )

    private fun mockMfaCheckSuccess(userId: UserId) {
        val userInternal = createTestUserInternal(id = userId)
        val userSession = createTestUserSessionInternal(userId = userId)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery { sessionManager.getUserSessionInternal(any()) } returns AppResult.Success(userSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(userInternal, userSession, true) } returns AppResult.Success(Unit)
    }

    @Test
    fun `successfully updates settings, logs audit and broadcasts via websocket`() = runTest {
        val settings = createTestManagementSecuritySettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val openSecuritySettings = createTestOpenSecuritySettings()

        mockMfaCheckSuccess(userId)

        coEvery { securitySettingsProvider.updateManagementSecuritySettings(settings) } returns AppResult.Success(Unit)
        every { securitySettingsProvider.getOpenSecuritySettings() } returns openSecuritySettings
        every { securitySettingsProvider.getManagementSecuritySettings() } returns settings

        val result = useCase(settings, context)

        assertEquals(AppResult.Success(settings), result)

        coVerify(exactly = 1) {
            securitySettingsProvider.updateManagementSecuritySettings(settings)
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
                resource = SecurityAuditResourceType.SECURITY_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.OPEN,
                frame = match {
                    it.type == SecurityWebSocketEventTypes.OPEN_SECURITY_SETTINGS_UPDATED
                }
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.MANAGEMENT,
                frame = match {
                    it.type == SecurityWebSocketEventTypes.MANAGEMENT_SECURITY_SETTINGS_UPDATED
                }
            )
        }
    }

    @Test
    fun `handles error, logs failure audit and does not broadcast`() = runTest {
        val settings = createTestManagementSecuritySettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val error = CommonError.Internal(RuntimeException("Database error"))

        mockMfaCheckSuccess(userId)

        coEvery { securitySettingsProvider.updateManagementSecuritySettings(settings) } returns AppResult.Error(error)

        val result = useCase(settings, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
                resource = SecurityAuditResourceType.SECURITY_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
        coVerify(exactly = 0) {
            securitySettingsProvider.getOpenSecuritySettings()
            securitySettingsProvider.getManagementSecuritySettings()
            webSocketManager.sendMessageToScope(any(), any())
        }
    }
}
