package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserSessionIdTest {

    @Test
    fun `generate creates unique ids`() {
        val id1 = UserSessionId.generate()
        val id2 = UserSessionId.generate()

        assertNotEquals(id1, id2)
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `asHexDashString returns canonical hex format`() {
        val id = UserSessionId.generate()
        val hex = id.asHexDashString()

        assertEquals(36, hex.length)
        assertEquals(4, hex.count { it == '-' })
        val parsed = Uuid.parse(hex)
        assertEquals(id.value, parsed)
    }

    @Test
    fun `UserSessionId round-trips through hex string`() {
        val id = UserSessionId.generate()
        val hex = id.asHexDashString()
        val parsed = hex.toUserSessionIdOrThrow()

        assertEquals(id, parsed)
        assertEquals(id.value, parsed.value)
    }

    @Test
    fun `toUserSessionIdOrNull returns null for invalid string`() {
        assertNull("not-a-uuid".toUserSessionIdOrNull())
        assertNull("".toUserSessionIdOrNull())
    }

    @Test
    fun `toUserSessionIdOrNull returns UserSessionId for valid hex string`() {
        val id = UserSessionId.generate()
        val parsed = id.asHexDashString().toUserSessionIdOrNull()
        assertEquals(id, parsed)
    }

    @Test
    fun `toUserSessionIdOrThrow parses valid hex string`() {
        val id = UserSessionId.generate()
        val parsed = id.asHexDashString().toUserSessionIdOrThrow()
        assertEquals(id, parsed)
    }
}
