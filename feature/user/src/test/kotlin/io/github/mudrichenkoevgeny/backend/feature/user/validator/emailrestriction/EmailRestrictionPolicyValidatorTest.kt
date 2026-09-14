package io.github.mudrichenkoevgeny.backend.feature.user.validator.emailrestriction

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.emailrestriction.createTestEmailRestrictionPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmailRestrictionPolicyValidatorTest {

    private val validator = EmailRestrictionPolicyValidator()

    @Test
    fun `isEmailAllowed returns true when restrictions are disabled`() {
        val policy = createTestEmailRestrictionPolicy()

        assertTrue(validator.isEmailAllowed("user@tempmail.com", policy))
    }

    @Test
    fun `isEmailAllowed rejects email matching @-mask in blacklist`() {
        val policy = createTestEmailRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("@tempmail.com", "@disposable.org")
        )

        assertFalse(validator.isEmailAllowed("john@tempmail.com", policy))
        assertFalse(validator.isEmailAllowed("alice@disposable.org", policy))
        assertTrue(validator.isEmailAllowed("john@gmail.com", policy))
    }

    @Test
    fun `isEmailAllowed rejects email matching domain-name in blacklist`() {
        val policy = createTestEmailRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("tempmail.com")
        )

        assertFalse(validator.isEmailAllowed("john@tempmail.com", policy))
        assertFalse(validator.isEmailAllowed("john@sub.tempmail.com", policy))
        assertTrue(validator.isEmailAllowed("john@gmail.com", policy))
    }

    @Test
    fun `isEmailAllowed rejects exact email match in blacklist`() {
        val policy = createTestEmailRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("spammer@gmail.com")
        )

        assertFalse(validator.isEmailAllowed("spammer@gmail.com", policy))
        assertTrue(validator.isEmailAllowed("gooduser@gmail.com", policy))
    }

    @Test
    fun `isEmailAllowed permits email present in whitelist and rejects others`() {
        val policy = createTestEmailRestrictionPolicy(
            isWhitelistEnabled = true,
            whitelist = listOf("@company.com", "partner.org")
        )

        assertTrue(validator.isEmailAllowed("employee@company.com", policy))
        assertTrue(validator.isEmailAllowed("partner@partner.org", policy))
        assertFalse(validator.isEmailAllowed("random@gmail.com", policy))
    }

    @Test
    fun `isEmailAllowed prioritizes blacklist when both blacklist and whitelist are enabled`() {
        val policy = createTestEmailRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("blocked@company.com"),
            isWhitelistEnabled = true,
            whitelist = listOf("@company.com")
        )

        assertFalse(validator.isEmailAllowed("blocked@company.com", policy))
        assertTrue(validator.isEmailAllowed("employee@company.com", policy))
        assertFalse(validator.isEmailAllowed("external@gmail.com", policy))
    }
}
