package io.github.mudrichenkoevgeny.backend.feature.settings.api.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.model.globalsettings.GlobalSettingsPayload
import io.mockk.any
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class UpdateGlobalSettingsUseCaseTest {

    private companion object {
        private const val IP_ADDRESS = "127.0.0.1"
    }

    private val globalSettings = GlobalSettings(
        privacyPolicyUrl = "privacy",
        termsOfServiceUrl = "tos",
        contactSupportEmail = "support@example.com"
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
    fun `invoke on success sends GLOBAL_SETTINGS_UPDATED to all sockets`() = runTest {
        val provider = mockk<GlobalSettingsProvider>()
        val auditLogger = mockk<AuditLogger>(relaxed = true)
        val webSocketManager = mockk<WebSocketManager>(relaxed = true)
        coEvery { provider.updateGlobalSettings(globalSettings) } returns AppResult.Success(Unit)

        val useCase = UpdateGlobalSettingsUseCase(provider, auditLogger, webSocketManager)
        val result = useCase(globalSettings, requestContext)

        assertEquals(AppResult.Success(Unit), result)
        val frameSlot = slot<SocketFrame>()
        coVerify(exactly = 1) { webSocketManager.sendMessageToAll(capture(frameSlot)) }
        val frame = frameSlot.captured
        assertEquals(SettingsWebSocketEventTypes.GLOBAL_SETTINGS_UPDATED, frame.type)
        assertNotNull(frame.payload)
        val decoded = FoundationJson.decodeFromJsonElement(
            GlobalSettingsPayload.serializer(),
            frame.payload!!
        )
        assertEquals(globalSettings.toGlobalSettingsPayload(), decoded)
    }

    @Test
    fun `invoke on provider error does not broadcast websocket`() = runTest {
        val provider = mockk<GlobalSettingsProvider>()
        val auditLogger = mockk<AuditLogger>(relaxed = true)
        val webSocketManager = mockk<WebSocketManager>(relaxed = true)
        val err = CommonError.Internal(Throwable("db"))
        coEvery { provider.updateGlobalSettings(globalSettings) } returns AppResult.Error(err)

        val useCase = UpdateGlobalSettingsUseCase(provider, auditLogger, webSocketManager)
        val result = useCase(globalSettings, requestContext)

        assertEquals(AppResult.Error(err), result)
        coVerify(exactly = 0) { webSocketManager.sendMessageToAll(any()) }
    }
}
