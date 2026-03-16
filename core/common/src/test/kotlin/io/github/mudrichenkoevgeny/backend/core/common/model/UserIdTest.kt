package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserIdTest {

    @Test
    fun `generate creates unique ids`() {
        val id1 = UserId.generate()
        val id2 = UserId.generate()

        assertNotEquals(id1, id2)
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `asHexDashString returns canonical hex format`() {
        val id = UserId.generate()
        val hex = id.asHexDashString()

        assertEquals(36, hex.length)
        assertEquals(4, hex.count { it == '-' })
        val parsed = Uuid.parse(hex)
        assertEquals(id.value, parsed)
    }

    @Test
    fun `UserId round-trips through hex string`() {
        val id = UserId.generate()
        val hex = id.asHexDashString()
        val parsed = hex.toUserIdOrThrow()

        assertEquals(id, parsed)
        assertEquals(id.value, parsed.value)
    }

    @Test
    fun `toUserIdOrNull returns null for invalid string`() {
        assertNull("not-a-uuid".toUserIdOrNull())
        assertNull("".toUserIdOrNull())
    }

    @Test
    fun `toUserIdOrNull returns UserId for valid hex string`() {
        val id = UserId.generate()
        val parsed = id.asHexDashString().toUserIdOrNull()
        assertEquals(id, parsed)
    }

    @Test
    fun `toUserIdOrThrow parses valid hex string`() {
        val id = UserId.generate()
        val parsed = id.asHexDashString().toUserIdOrThrow()
        assertEquals(id, parsed)
    }
}
