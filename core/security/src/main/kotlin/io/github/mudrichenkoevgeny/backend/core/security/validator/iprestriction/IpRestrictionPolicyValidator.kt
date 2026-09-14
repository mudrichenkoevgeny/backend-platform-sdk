package io.github.mudrichenkoevgeny.backend.core.security.validator.iprestriction

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import java.math.BigInteger
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates client IP addresses against an [IpRestrictionPolicy].
 */
@Singleton
class IpRestrictionPolicyValidator @Inject constructor() {

    /**
     * Checks if the given [ipAddress] is permitted by the provided [policy].
     *
     * @param ipAddress Client IP address string (IPv4 or IPv6).
     * @param policy IP restriction policy containing blacklist and whitelist settings.
     * @return `true` if the IP address is allowed, `false` if it is restricted.
     */
    fun isIpAllowed(ipAddress: String, policy: IpRestrictionPolicy): Boolean {
        if (!policy.isBlacklistEnabled && !policy.isWhitelistEnabled) {
            return true
        }

        val clientIp = ipAddress.trim()
        if (clientIp.isEmpty()) {
            return !policy.isWhitelistEnabled
        }

        if (policy.isBlacklistEnabled) {
            val isBlacklisted = policy.blacklist.any { pattern -> matchesIp(clientIp, pattern) }
            if (isBlacklisted) {
                return false
            }
        }

        if (policy.isWhitelistEnabled) {
            val isWhitelisted = policy.whitelist.any { pattern -> matchesIp(clientIp, pattern) }
            if (!isWhitelisted) {
                return false
            }
        }

        return true
    }

    private fun matchesIp(clientIp: String, pattern: String): Boolean {
        val trimmedPattern = pattern.trim()
        if (trimmedPattern.isEmpty()) {
            return false
        }

        if (clientIp.equals(trimmedPattern, ignoreCase = true)) {
            return true
        }

        if (trimmedPattern.contains("/")) {
            return matchesCidr(clientIp, trimmedPattern)
        }

        return false
    }

    private fun matchesCidr(clientIp: String, cidrPattern: String): Boolean {
        return try {
            val parts = cidrPattern.split("/")
            if (parts.size != 2) {
                return false
            }

            val prefixLength = parts[1].toIntOrNull() ?: return false
            val networkAddress = InetAddress.getByName(parts[0])
            val clientAddress = InetAddress.getByName(clientIp)

            if (networkAddress.javaClass != clientAddress.javaClass) {
                return false
            }

            val networkBytes = networkAddress.address
            val clientBytes = clientAddress.address

            val totalBits = networkBytes.size * 8
            if (prefixLength !in 0..totalBits) {
                return false
            }

            val networkInt = BigInteger(1, networkBytes)
            val clientInt = BigInteger(1, clientBytes)

            val mask = if (prefixLength == 0) {
                BigInteger.ZERO
            } else {
                (BigInteger.ONE.shl(totalBits) - BigInteger.ONE).shl(totalBits - prefixLength)
            }

            clientInt.and(mask) == networkInt.and(mask)
        } catch (_: Exception) {
            false
        }
    }
}
