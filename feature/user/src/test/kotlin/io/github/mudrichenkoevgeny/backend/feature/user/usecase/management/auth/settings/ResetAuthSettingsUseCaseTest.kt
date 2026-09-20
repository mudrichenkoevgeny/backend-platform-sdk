package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.settings.createTestManagementAuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResetAuthSettingsUseCaseTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = ResetAuthSettingsUseCase(
        authSettingsProvider = authSettingsProvider,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        webSocketManager = webSocketManager
    )

    @Test
    fun `successfully resets settings, logs audit and broadcasts via websocket`() = runTest {
        val defaultSettings = createTestManagementAuthSettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val openAuthSettings = mockk<OpenAuthSettings>(relaxed = true)

        coEvery {
            authSettingsProvider.resetManagementAuthSettings()
        } returns AppResult.Success(defaultSettings)

        every { authSettingsProvider.getOpenAuthSettings() } returns openAuthSettings

        val result = useCase(context)

        assertEquals(AppResult.Success(defaultSettings), result)

        coVerify(exactly = 1) {
            authSettingsProvider.resetManagementAuthSettings()
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = UserAuditActionType.MANAGEMENT_RESET_AUTH_SETTINGS,
                resource = UserAuditResourceType.AUTH_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.OPEN,
                frame = match {
                    it.type == UserWebSocketEventTypes.OPEN_AUTH_SETTINGS_UPDATED
                }
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.MANAGEMENT,
                frame = match {
                    it.type == UserWebSocketEventTypes.MANAGEMENT_AUTH_SETTINGS_UPDATED
                }
            )
        }
    }

    @Test
    fun `handles error, logs failure audit and does not broadcast`() = runTest {
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val error = UserError.UserForbidden()
        val errorLogData = AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        coEvery {
            authSettingsProvider.resetManagementAuthSettings()
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = UserAuditActionType.MANAGEMENT_RESET_AUTH_SETTINGS,
                resource = UserAuditResourceType.AUTH_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }

        coVerify(exactly = 0) {
            authSettingsProvider.getOpenAuthSettings()
            webSocketManager.sendMessageToScope(any(), any())
        }
    }
}
