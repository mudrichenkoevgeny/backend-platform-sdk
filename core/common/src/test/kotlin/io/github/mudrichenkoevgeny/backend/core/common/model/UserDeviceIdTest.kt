package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserDeviceIdTest {

    @Test
    fun `generate creates unique ids`() {
        val id1 = UserDeviceId.generate()
        val id2 = UserDeviceId.generate()

        assertNotEquals(id1, id2)
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `asHexDashString returns underlying value`() {
        val id = UserDeviceId("device-abc")
        assertEquals("device-abc", id.asHexDashString())
    }

    @Test
    fun `UserDeviceId round-trips through string`() {
        val id = UserDeviceId.generate()
        val str = id.asHexDashString()
        val parsed = str.toUserDeviceIdOrThrow()
        assertEquals(id, parsed)
    }

    @Test
    fun `toUserDeviceIdOrNull returns null for blank string`() {
        assertNull("".toUserDeviceIdOrNull())
        assertNull("   ".toUserDeviceIdOrNull())
    }

    @Test
    fun `toUserDeviceIdOrNull returns UserDeviceId for non-blank string`() {
        val id = "device-123".toUserDeviceIdOrNull()
        assertEquals(UserDeviceId("device-123"), id)
    }

    @Test
    fun `toUserDeviceIdOrThrow throws for blank string`() {
        assertThrows<IllegalArgumentException> { "".toUserDeviceIdOrThrow() }
        assertThrows<IllegalArgumentException> { "   ".toUserDeviceIdOrThrow() }
    }

    @Test
    fun `toUserDeviceIdOrThrow parses valid string`() {
        val id = "device-xyz".toUserDeviceIdOrThrow()
        assertEquals(UserDeviceId("device-xyz"), id)
    }
}
