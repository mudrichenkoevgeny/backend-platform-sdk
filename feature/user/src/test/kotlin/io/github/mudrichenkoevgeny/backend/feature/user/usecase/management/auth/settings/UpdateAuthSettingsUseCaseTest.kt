package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
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

class UpdateAuthSettingsUseCaseTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = UpdateAuthSettingsUseCase(
        authSettingsProvider = authSettingsProvider,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        webSocketManager = webSocketManager
    )

    private fun sampleManagementSettings() = ManagementAuthSettings(
        availableAuthProviders = mockk(),
        maxTotalIdentifiers = 5,
        maxEmailIdentifiers = 2,
        maxPhoneIdentifiers = 2,
        maxIdentifiersPerExternalProvider = 1,
        maxActiveSessions = 10,
        accessTokenExpirationSeconds = 3600,
        refreshTokenExpirationSeconds = 86400,
        accountDeletionDelaySeconds = 2592000
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
        val settings = sampleManagementSettings()
        val userId = UserId.generate()
        val context = authContext(userId)
        val publicSettings = mockk<PublicAuthSettings>(relaxed = true)

        coEvery {
            authSettingsProvider.updateManagementAuthSettings(settings)
        } returns AppResult.Success(Unit)

        every { authSettingsProvider.getPublicAuthSettings() } returns publicSettings

        val result = useCase(settings, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify(exactly = 1) {
            authSettingsProvider.updateManagementAuthSettings(settings)
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                resource = UserAuditResourceType.AUTH_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToAll(match {
                it.type == UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED
            })
        }
    }

    @Test
    fun `handles error, logs failure audit and does not broadcast`() = runTest {
        val settings = sampleManagementSettings()
        val userId = UserId.generate()
        val context = authContext(userId)
        val error = UserError.UserForbidden()
        val errorLogData = AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        coEvery {
            authSettingsProvider.updateManagementAuthSettings(settings)
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(settings, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                resource = UserAuditResourceType.AUTH_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }

        coVerify(exactly = 0) {
            authSettingsProvider.getPublicAuthSettings()
            webSocketManager.sendMessageToAll(any())
        }
    }
}