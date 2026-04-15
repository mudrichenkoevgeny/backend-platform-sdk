package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.settings.PublicAuthSettingsPayload
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class UpdateAuthSettingsUseCaseTest {

    private companion object {
        private const val IP_ADDRESS = "127.0.0.1"
    }

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = UpdateAuthSettingsUseCase(
        authSettingsProvider = authSettingsProvider,
        auditLogger = auditLogger,
        webSocketManager = webSocketManager
    )

    private val requestContext = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = ClientInfo(
            deviceInfo = ClientDeviceInfo(
                deviceId = null,
                deviceName = null,
                clientType = null,
                language = null,
                appVersion = null,
                operationSystemVersion = null
            ),
            userAgent = null,
            ipAddress = IP_ADDRESS,
            host = null,
            origin = null,
            apiVersion = null
        )
    )

    @Test
    fun `invoke on success sends AUTH_SETTINGS_UPDATED with public payload`() = runTest {
        val management = sampleManagementAuthSettings()
        val publicAfterUpdate = PublicAuthSettings(
            availableAuthProviders = management.availableAuthProviders
        )

        coEvery { authSettingsProvider.updateManagementAuthSettings(management) } returns AppResult.Success(Unit)
        every { authSettingsProvider.getPublicAuthSettings() } returns AppResult.Success(publicAfterUpdate)

        val result = useCase.invoke(management, requestContext)

        assertEquals(AppResult.Success(Unit), result)

        verify(exactly = 1) {
            auditLogger.log(
                any(),
                AuditActorType.USER,
                any(),
                UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                UserAuditResourceType.AUTH_SETTINGS,
                any(),
                AuditStatus.SUCCESS,
                any(),
                any()
            )
        }

        val frameSlot = slot<SocketFrame>()
        coVerify(exactly = 1) { webSocketManager.sendMessageToAll(capture(frameSlot)) }
        val frame = frameSlot.captured
        assertEquals(UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED, frame.type)
        assertNotNull(frame.payload)
        val decoded = FoundationJson.decodeFromJsonElement(
            PublicAuthSettingsPayload.serializer(),
            frame.payload!!
        )
        assertEquals(publicAfterUpdate.toAuthSettingsPayload(), decoded)
    }

    @Test
    fun `invoke on provider error logs failed audit and does not broadcast`() = runTest {
        val management = sampleManagementAuthSettings()
        val error = CommonError.Unknown(message = "persist failed")

        coEvery { authSettingsProvider.updateManagementAuthSettings(management) } returns AppResult.Error(error)

        val result = useCase.invoke(management, requestContext)

        assertEquals(AppResult.Error(error), result)

        verify(exactly = 1) {
            auditLogger.log(
                any(),
                AuditActorType.USER,
                any(),
                UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                UserAuditResourceType.AUTH_SETTINGS,
                any(),
                AuditStatus.FAILED,
                any(),
                any()
            )
        }

        coVerify(exactly = 0) { webSocketManager.sendMessageToAll(any()) }
        verify(exactly = 0) { authSettingsProvider.getPublicAuthSettings() }
    }

    @Test
    fun `invoke does not broadcast when update succeeds but public settings read fails`() = runTest {
        val management = sampleManagementAuthSettings()
        val readError = CommonError.Unknown(message = "read failed")

        coEvery { authSettingsProvider.updateManagementAuthSettings(management) } returns AppResult.Success(Unit)
        every { authSettingsProvider.getPublicAuthSettings() } returns AppResult.Error(readError)

        val result = useCase.invoke(management, requestContext)

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 0) { webSocketManager.sendMessageToAll(any()) }
        verify(exactly = 1) { authSettingsProvider.getPublicAuthSettings() }
    }

    private fun sampleManagementAuthSettings() = ManagementAuthSettings(
        availableAuthProviders = AvailableAuthProviders(
            primary = listOf(UserAuthProvider.EMAIL),
            secondary = listOf(UserAuthProvider.GOOGLE)
        ),
        accessTokenValidityHours = 24L,
        refreshTokenValidityDays = 30L
    )
}
