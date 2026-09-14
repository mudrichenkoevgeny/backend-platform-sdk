package io.github.mudrichenkoevgeny.backend.feature.user.validator.emailrestriction

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.emailrestriction.EmailRestrictionPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates email addresses against an [EmailRestrictionPolicy].
 */
@Singleton
class EmailRestrictionPolicyValidator @Inject constructor() {

    /**
     * Checks if the given [email] address is permitted by the provided [policy].
     *
     * @param email Email address string.
     * @param policy Email restriction policy containing blacklist and whitelist settings.
     * @return `true` if the email address is allowed, `false` if it is restricted.
     */
    fun isEmailAllowed(email: String, policy: EmailRestrictionPolicy): Boolean {
        if (!policy.isBlacklistEnabled && !policy.isWhitelistEnabled) {
            return true
        }

        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isEmpty()) {
            return !policy.isWhitelistEnabled
        }

        if (policy.isBlacklistEnabled) {
            val isBlacklisted = policy.blacklist.any { pattern -> matchesEmail(normalizedEmail, pattern) }
            if (isBlacklisted) {
                return false
            }
        }

        if (policy.isWhitelistEnabled) {
            val isWhitelisted = policy.whitelist.any { pattern -> matchesEmail(normalizedEmail, pattern) }
            if (!isWhitelisted) {
                return false
            }
        }

        return true
    }

    private fun matchesEmail(email: String, pattern: String): Boolean {
        val normalizedPattern = pattern.trim().lowercase()
        if (normalizedPattern.isEmpty()) {
            return false
        }

        val domain = email.substringAfter('@', "")

        return when {
            normalizedPattern.startsWith("@") -> {
                val patternDomain = normalizedPattern.removePrefix("@")
                email.endsWith(normalizedPattern) || (domain == patternDomain)
            }
            normalizedPattern.startsWith("*@") -> {
                val patternDomain = normalizedPattern.removePrefix("*@")
                (domain == patternDomain) || domain.endsWith(".$patternDomain")
            }
            normalizedPattern.startsWith("*.") -> {
                val patternDomain = normalizedPattern.removePrefix("*.")
                (domain == patternDomain) || domain.endsWith(".$patternDomain")
            }
            normalizedPattern.contains("@") -> {
                email == normalizedPattern
            }
            else -> {
                (domain == normalizedPattern) || domain.endsWith(".$normalizedPattern")
            }
        }
    }
}
