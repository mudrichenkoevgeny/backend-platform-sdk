package io.github.mudrichenkoevgeny.backend.feature.settingsapi.api.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateGlobalSettingsUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = UpdateGlobalSettingsUseCase(
        globalSettingsProvider,
        auditLogger,
        auditErrorConverter,
        webSocketManager
    )

    private fun sampleSettings() = GlobalSettings(
        privacyPolicyUrl = "https://example.com/privacy",
        termsOfServiceUrl = "https://example.com/terms",
        contactSupportEmail = "support@example.com"
    )

    private fun authContext(userId: UserId) = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo(
            deviceInfo = ClientDeviceInfo(null, null, null, null, null, null),
            userAgent = "test-agent",
            ipAddress = "127.0.0.1",
            host = null,
            origin = null,
            apiVersion = null
        )
    )

    @Test
    fun `successfully updates settings, logs audit and broadcasts via websocket`() = runTest {
        val settings = sampleSettings()
        val userId = UserId.generate()
        val context = authContext(userId)

        coEvery { globalSettingsProvider.updateGlobalSettings(settings) } returns AppResult.Success(Unit)

        val result = useCase(settings, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify(exactly = 1) {
            globalSettingsProvider.updateGlobalSettings(settings)
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SettingsAuditActionType.MANAGEMENT_UPDATE_GLOBAL_SETTINGS,
                resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToAll(match {
                it.type == SettingsWebSocketEventTypes.GLOBAL_SETTINGS_UPDATED
            })
        }
    }

    @Test
    fun `handles error, logs failure audit and does not broadcast`() = runTest {
        val settings = sampleSettings()
        val userId = UserId.generate()
        val context = authContext(userId)
        val error = CommonError.Internal(RuntimeException("Database error"))
        val errorLogData = AuditErrorLogData(AuditStatus.FAILED, emptySet())

        coEvery { globalSettingsProvider.updateGlobalSettings(settings) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(settings, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SettingsAuditActionType.MANAGEMENT_UPDATE_GLOBAL_SETTINGS,
                resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
        coVerify(exactly = 0) {
            webSocketManager.sendMessageToAll(any())
        }
    }
}