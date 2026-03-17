package io.github.mudrichenkoevgeny.backend.feature.user.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdentifierMaskerUtilTest {

    @Test
    fun `maskEmail returns fallback for malformed input`() {
        assertEquals(IdentifierMaskerUtil.LARGE_MASK, IdentifierMaskerUtil.maskEmail("not-an-email"))
        assertEquals(IdentifierMaskerUtil.LARGE_MASK, IdentifierMaskerUtil.maskEmail("a@b@c"))
    }

    @Test
    fun `maskEmail masks local and domain parts`() {
        assertEquals("*@*.com", IdentifierMaskerUtil.maskEmail("a@b.com"))
        assertEquals("a*@c*.com", IdentifierMaskerUtil.maskEmail("ab@cd.com"))
        assertEquals("a***@e***.com", IdentifierMaskerUtil.maskEmail("alex@example.com"))
    }

    @Test
    fun `maskPhone keeps last 4 digits and removes formatting`() {
        assertEquals(IdentifierMaskerUtil.LARGE_MASK, IdentifierMaskerUtil.maskPhone("12"))
        assertEquals("+***4567", IdentifierMaskerUtil.maskPhone("+1 (234) 567"))
        assertEquals("+***7890", IdentifierMaskerUtil.maskPhone("00-11-22-33-44-55-66-77-88-99-00-7890"))
    }

    @Test
    fun `maskExternal keeps first 2 characters`() {
        assertEquals(IdentifierMaskerUtil.LARGE_MASK, IdentifierMaskerUtil.maskExternal("abc"))
        assertEquals("ab***", IdentifierMaskerUtil.maskExternal("abcdef"))
    }
}

