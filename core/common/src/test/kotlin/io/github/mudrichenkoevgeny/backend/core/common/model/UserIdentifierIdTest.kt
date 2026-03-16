package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserIdentifierIdTest {

    @Test
    fun `generate creates unique ids`() {
        val id1 = UserIdentifierId.generate()
        val id2 = UserIdentifierId.generate()

        assertNotEquals(id1, id2)
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `asHexDashString returns canonical hex format`() {
        val id = UserIdentifierId.generate()
        val hex = id.asHexDashString()

        assertEquals(36, hex.length)
        assertEquals(4, hex.count { it == '-' })
        val parsed = Uuid.parse(hex)
        assertEquals(id.value, parsed)
    }

    @Test
    fun `UserIdentifierId round-trips through hex string`() {
        val id = UserIdentifierId.generate()
        val hex = id.asHexDashString()
        val parsed = hex.toUserIdentifierIdOrThrow()

        assertEquals(id, parsed)
        assertEquals(id.value, parsed.value)
    }

    @Test
    fun `toUserIdentifierIdOrNull returns null for invalid string`() {
        assertNull("not-a-uuid".toUserIdentifierIdOrNull())
        assertNull("".toUserIdentifierIdOrNull())
    }

    @Test
    fun `toUserIdentifierIdOrNull returns UserIdentifierId for valid hex string`() {
        val id = UserIdentifierId.generate()
        val parsed = id.asHexDashString().toUserIdentifierIdOrNull()
        assertEquals(id, parsed)
    }

    @Test
    fun `toUserIdentifierIdOrThrow parses valid hex string`() {
        val id = UserIdentifierId.generate()
        val parsed = id.asHexDashString().toUserIdentifierIdOrThrow()
        assertEquals(id, parsed)
    }
}
