package io.github.mudrichenkoevgeny.backend.core.common.mask

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataMaskerTest {

    @Test
    fun `maskEmail returns fallback for malformed input`() {
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskEmail("not-an-email"))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskEmail("a@b@c"))
    }

    @Test
    fun `maskEmail masks local and domain parts`() {
        assertEquals("*@*.com", DataMasker.maskEmail("a@b.com"))
        assertEquals("a*@c*.com", DataMasker.maskEmail("ab@cd.com"))
        assertEquals("a***@e***.com", DataMasker.maskEmail("alex@example.com"))
    }

    @Test
    fun `maskPhone keeps last 4 digits and removes formatting`() {
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskPhone("12"))
        assertEquals("+***4567", DataMasker.maskPhone("+1 (234) 567"))
        assertEquals("+***7890", DataMasker.maskPhone("00-11-22-33-44-55-66-77-88-99-00-7890"))
    }

    @Test
    fun `maskId keeps first 2 characters`() {
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskId("abc"))
        assertEquals("ab***", DataMasker.maskId("abcdef"))
    }

    @Test
    fun `maskIp ipv4 keeps first octet`() {
        assertEquals("192.*.*.*", DataMasker.maskIpAddress("192.168.1.100"))
        assertEquals("10.*.*.*", DataMasker.maskIpAddress(" 10.0.0.1 "))
    }

    @Test
    fun `maskIp ipv6 keeps first two hextets when present in textual form`() {
        assertEquals("2001:db8:***", DataMasker.maskIpAddress("2001:db8::1"))
    }

    @Test
    fun `maskIp strips zone id and brackets for literals`() {
        assertEquals("fe80:0:***", DataMasker.maskIpAddress("fe80::1%eth0"))
        assertEquals("2001:db8:***", DataMasker.maskIpAddress("[2001:db8::1]"))
    }

    @Test
    fun `maskIp returns fallback for invalid input`() {
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress(""))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress("   "))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress("not-an-ip"))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress("999.999.999.999"))
    }
}
