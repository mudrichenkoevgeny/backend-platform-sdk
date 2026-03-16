package io.github.mudrichenkoevgeny.backend.core.common.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CommonModesTest {

    @Test
    fun `UserId generate creates valid uuid string`() {
        val id = UserId.generate()

        val parsed = id.asHexDashString().toUserIdOrThrow()
        assertEquals(id, parsed)
    }

    @Test
    fun `toUserIdOrNull returns null for invalid input`() {
        assertNull("not-a-uuid".toUserIdOrNull())
    }

    @Test
    fun `UserSessionId round-trips through string`() {
        val id = UserSessionId.generate()

        val parsed = id.asHexDashString().toUserSessionIdOrThrow()
        assertEquals(id, parsed)
    }

    @Test
    fun `toUserSessionIdOrNull returns null for invalid input`() {
        assertNull("not-a-uuid".toUserSessionIdOrNull())
    }

    @Test
    fun `UserIdentifierId round-trips through string`() {
        val id = UserIdentifierId.generate()

        val parsed = id.asHexDashString().toUserIdentifierIdOrThrow()
        assertEquals(id, parsed)
    }

    @Test
    fun `toUserIdentifierIdOrNull returns null for invalid input`() {
        assertNull("not-a-uuid".toUserIdentifierIdOrNull())
    }

    @Test
    fun `UserDeviceId holds raw value`() {
        val deviceId = UserDeviceId("device-123")

        assertNotNull(deviceId)
        assertEquals("device-123", deviceId.value)
    }
}

