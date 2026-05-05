package io.github.mudrichenkoevgeny.backend.core.common.mask

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataMaskerTest {

    @Test
    fun `maskPartialValue trims and masks by length`() {
        assertEquals("", DataMasker.maskPartialValue("   "))
        assertEquals(DataMasker.SMALL_MASK, DataMasker.maskPartialValue("x"))
        assertEquals("x${DataMasker.SMALL_MASK}", DataMasker.maskPartialValue("xy"))
        assertEquals("a${DataMasker.LARGE_MASK}z", DataMasker.maskPartialValue("abcz"))
    }

    @Test
    fun `maskFullValue blanks pass through trimmed blank non-blank becomes large mask`() {
        assertEquals("", DataMasker.maskFullValue(""))
        assertEquals("", DataMasker.maskFullValue("   "))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskFullValue("secret"))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskFullValue("  peek  "))
    }

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
        val res1 = DataMasker.maskIpAddress("192.168.1.100")
        val res2 = DataMasker.maskIpAddress(" 10.0.0.1 ")

        assertEquals("192.*.*.*", res1)
        assertEquals("10.*.*.*", res2)
    }

    @Test
    fun `maskIp ipv6 keeps first two hextets from normalized form`() {
        val result = DataMasker.maskIpAddress("2001:db8::1")
        assertEquals(true, result.contains(":"))
    }

    @Test
    fun `maskIp strips zone id and brackets for literals`() {
        val result = DataMasker.maskIpAddress("fe80::1%eth0")
        assertEquals(true, result.contains(":"))

        val resultBrackets = DataMasker.maskIpAddress("[2001:db8::1]")
        assertEquals(true, resultBrackets.contains(":"))
    }

    @Test
    fun `maskIp returns fallback for invalid input`() {
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress(""))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress("   "))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress("not-an-ip"))
        assertEquals(DataMasker.LARGE_MASK, DataMasker.maskIpAddress("999.999.999.999"))
    }
}