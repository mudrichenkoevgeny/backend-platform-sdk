package io.github.mudrichenkoevgeny.backend.core.security.validator.iprestriction

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IpRestrictionPolicyValidatorTest {

    private val validator = IpRestrictionPolicyValidator()

    @Test
    fun `isIpAllowed returns true when both blacklist and whitelist are disabled`() {
        val policy = IpRestrictionPolicy(
            isBlacklistEnabled = false,
            blacklist = listOf("192.168.1.1"),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        assertTrue(validator.isIpAllowed("192.168.1.1", policy))
        assertTrue(validator.isIpAllowed("10.0.0.1", policy))
    }

    @Test
    fun `isIpAllowed rejects IP matching exact blacklist entry`() {
        val policy = IpRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("192.168.1.50", "10.0.0.1"),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        assertFalse(validator.isIpAllowed("192.168.1.50", policy))
        assertFalse(validator.isIpAllowed("10.0.0.1", policy))
        assertTrue(validator.isIpAllowed("192.168.1.51", policy))
    }

    @Test
    fun `isIpAllowed rejects IP within blacklisted IPv4 CIDR range`() {
        val policy = IpRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("192.168.1.0/24"),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        assertFalse(validator.isIpAllowed("192.168.1.1", policy))
        assertFalse(validator.isIpAllowed("192.168.1.254", policy))
        assertTrue(validator.isIpAllowed("192.168.2.1", policy))
    }

    @Test
    fun `isIpAllowed rejects IP within blacklisted IPv6 CIDR range`() {
        val policy = IpRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("2001:db8::/32"),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        assertFalse(validator.isIpAllowed("2001:db8::1", policy))
        assertFalse(validator.isIpAllowed("2001:db8:ffff:ffff::1", policy))
        assertTrue(validator.isIpAllowed("2001:db9::1", policy))
    }

    @Test
    fun `isIpAllowed permits IP present in whitelist and rejects others`() {
        val policy = IpRestrictionPolicy(
            isBlacklistEnabled = false,
            blacklist = emptyList(),
            isWhitelistEnabled = true,
            whitelist = listOf("10.0.0.0/8", "172.16.0.1")
        )

        assertTrue(validator.isIpAllowed("10.1.2.3", policy))
        assertTrue(validator.isIpAllowed("172.16.0.1", policy))
        assertFalse(validator.isIpAllowed("172.16.0.2", policy))
        assertFalse(validator.isIpAllowed("192.168.1.1", policy))
    }

    @Test
    fun `isIpAllowed prioritizes blacklist when both blacklist and whitelist are enabled`() {
        val policy = IpRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("10.0.0.50"),
            isWhitelistEnabled = true,
            whitelist = listOf("10.0.0.0/24")
        )

        assertFalse(validator.isIpAllowed("10.0.0.50", policy))
        assertTrue(validator.isIpAllowed("10.0.0.1", policy))
        assertFalse(validator.isIpAllowed("10.0.1.1", policy))
    }
}
