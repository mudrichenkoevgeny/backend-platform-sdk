package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.securitysettings.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings
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
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = UpdateSecuritySettingsUseCase(
        securitySettingsProvider,
        auditLogger,
        auditErrorConverter,
        webSocketManager
    )

    @Test
    fun `successfully updates settings, logs audit and broadcasts via websocket`() = runTest {
        val settings = createTestManagementSecuritySettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val openSecuritySettings = mockk<OpenSecuritySettings>(relaxed = true)

        coEvery { securitySettingsProvider.updateManagementSecuritySettings(settings) } returns AppResult.Success(Unit)
        every { securitySettingsProvider.getOpenSecuritySettings() } returns openSecuritySettings
        every { securitySettingsProvider.getManagementSecuritySettings() } returns settings

        val result = useCase(settings, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify(exactly = 1) {
            securitySettingsProvider.updateManagementSecuritySettings(settings)
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
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
        val errorLogData = AuditErrorLogData(AuditStatus.FAILED, emptySet())

        coEvery { securitySettingsProvider.updateManagementSecuritySettings(settings) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(settings, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
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
