package io.github.mudrichenkoevgeny.backend.core.common.model.client

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class DeviceIdResolverTest {

    @Test
    fun `resolveDeviceId returns explicit deviceId when present in clientInfo`() {
        val explicitDeviceId = ClientDeviceId(Uuid.random())
        val clientInfo = createTestClientInfo(deviceId = explicitDeviceId)

        val resolved = DeviceIdResolver.resolveDeviceId("user-123", clientInfo)

        assertEquals(explicitDeviceId, resolved)
    }

    @Test
    fun `resolveDeviceId generates deterministic fallback deviceId when deviceId is null`() {
        val clientInfo1 = createTestClientInfo(
            deviceId = null,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )
        val clientInfo2 = createTestClientInfo(
            deviceId = null,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        val resolved1 = DeviceIdResolver.resolveDeviceId("user-123", clientInfo1)
        val resolved2 = DeviceIdResolver.resolveDeviceId("user-123", clientInfo2)

        assertEquals(resolved1, resolved2)
    }

    @Test
    fun `resolveDeviceId generates different fallback deviceId when userId changes`() {
        val clientInfo = createTestClientInfo(
            deviceId = null,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        val resolvedUser1 = DeviceIdResolver.resolveDeviceId("user-1", clientInfo)
        val resolvedUser2 = DeviceIdResolver.resolveDeviceId("user-2", clientInfo)

        assertNotEquals(resolvedUser1, resolvedUser2)
    }

    @Test
    fun `resolveDeviceId generates different fallback deviceId when ipAddress changes`() {
        val clientInfoIp1 = createTestClientInfo(
            deviceId = null,
            ipAddress = "10.0.0.1",
            userAgent = "Mozilla/5.0"
        )
        val clientInfoIp2 = createTestClientInfo(
            deviceId = null,
            ipAddress = "10.0.0.2",
            userAgent = "Mozilla/5.0"
        )

        val resolvedIp1 = DeviceIdResolver.resolveDeviceId("user-123", clientInfoIp1)
        val resolvedIp2 = DeviceIdResolver.resolveDeviceId("user-123", clientInfoIp2)

        assertNotEquals(resolvedIp1, resolvedIp2)
    }

    @Test
    fun `resolveDeviceId generates different fallback deviceId when userAgent changes`() {
        val clientInfoUa1 = createTestClientInfo(
            deviceId = null,
            ipAddress = "192.168.1.1",
            userAgent = "Chrome/100"
        )
        val clientInfoUa2 = createTestClientInfo(
            deviceId = null,
            ipAddress = "192.168.1.1",
            userAgent = "Firefox/100"
        )

        val resolvedUa1 = DeviceIdResolver.resolveDeviceId("user-123", clientInfoUa1)
        val resolvedUa2 = DeviceIdResolver.resolveDeviceId("user-123", clientInfoUa2)

        assertNotEquals(resolvedUa1, resolvedUa2)
    }

    @Test
    fun `resolveDeviceId handles null ipAddress and userAgent with defaults`() {
        val clientInfoNulls = createTestClientInfo(
            deviceId = null,
            ipAddress = null,
            userAgent = null
        )

        val resolved1 = DeviceIdResolver.resolveDeviceId("user-123", clientInfoNulls)
        val resolved2 = DeviceIdResolver.resolveDeviceId("user-123", clientInfoNulls)

        assertEquals(resolved1, resolved2)
    }

    private fun createTestClientInfo(
        deviceId: ClientDeviceId?,
        ipAddress: String? = "127.0.0.1",
        userAgent: String? = "TestAgent"
    ): ClientInfo = ClientInfo(
        clientDeviceInfo = ClientDeviceInfo(
            clientType = ClientType.WEB,
            deviceId = deviceId,
            deviceName = "Test Device",
            appVersion = "1.0.0",
            operationSystemVersion = "Linux",
            language = "en"
        ),
        userAgent = userAgent,
        ipAddress = ipAddress,
        host = "localhost",
        origin = "http://localhost",
        apiVersion = "v1"
    )
}
