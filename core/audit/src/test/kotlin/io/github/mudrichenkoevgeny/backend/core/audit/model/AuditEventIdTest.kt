package io.github.mudrichenkoevgeny.backend.core.audit.model

import kotlin.uuid.Uuid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuditEventIdTest {

    @Test
    fun `generate creates unique ids`() {
        val id1 = AuditEventId.generate()
        val id2 = AuditEventId.generate()

        assertNotEquals(id1, id2)
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `asHexDashString returns canonical hex format`() {
        val id = AuditEventId.generate()
        val hex = id.asHexDashString()

        assertEquals(36, hex.length)
        assertEquals(4, hex.count { it == '-' })
        val parsed = Uuid.parse(hex)
        assertEquals(id.value, parsed)
    }

    @Test
    fun `AuditEventId round-trips through hex string`() {
        val id = AuditEventId.generate()
        val hex = id.asHexDashString()
        val parsed = AuditEventId(Uuid.parse(hex))

        assertEquals(id, parsed)
        assertEquals(id.value, parsed.value)
    }

    @Test
    fun `toAuditEventIdOrNull returns null for invalid string`() {
        assertNull("not-a-uuid".toAuditEventIdOrNull())
        assertNull("".toAuditEventIdOrNull())
    }

    @Test
    fun `toAuditEventIdOrNull returns AuditEventId for valid hex string`() {
        val id = AuditEventId.generate()
        val parsed = id.asHexDashString().toAuditEventIdOrNull()
        assertEquals(id, parsed)
    }

    @Test
    fun `toAuditEventIdOrThrow parses valid hex string`() {
        val id = AuditEventId.generate()
        val parsed = id.asHexDashString().toAuditEventIdOrThrow()
        assertEquals(id, parsed)
    }

    @Test
    fun `toAuditEventIdOrThrow throws for invalid string`() {
        assertThrows<IllegalArgumentException> { "invalid".toAuditEventIdOrThrow() }
    }
}
