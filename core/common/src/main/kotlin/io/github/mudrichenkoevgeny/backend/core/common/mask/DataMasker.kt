package io.github.mudrichenkoevgeny.backend.core.common.mask

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Masks personally identifiable values for logs, audit metadata, and API responses.
 *
 * The rules are intentionally simple and deterministic:
 * - they keep a small prefix/suffix when possible,
 * - they avoid leaking full values,
 * - they return a fallback mask for malformed inputs.
 */
object DataMasker {

    const val SMALL_MASK = "*"
    const val LARGE_MASK = "***"

    /**
     * Values already classified as partially safe to correlate: keeps the first and last character when possible,
     * masks the middle with [LARGE_MASK]. Shorter inputs collapse to [SMALL_MASK] / a single visible edge as needed.
     */
    fun maskPartialValue(value: String): String {
        val v = value.trim()
        if (v.isEmpty()) return v
        return when (v.length) {
            1 -> SMALL_MASK
            2 -> "${v.first()}$SMALL_MASK"
            else -> "${v.first()}$LARGE_MASK${v.last()}"
        }
    }

    /**
     * Values that must not be revealed: any non-blank string becomes [LARGE_MASK]; blank stays blank.
     */
    fun maskFullValue(value: String): String =
        if (value.isBlank()) value.trim() else LARGE_MASK

    /**
     * Masks an email address keeping only a minimal prefix of local and domain parts.
     *
     * Examples:
     * - `a@b.com` -> `*@*.com`
     * - `ab@cd.com` -> `a*@c*.com`
     * - `alex@example.com` -> `a***@e***.com`
     *
     * If the input cannot be split into exactly two parts by `@`, returns [LARGE_MASK].
     */
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return LARGE_MASK

        val local = parts[0]
        val domain = parts[1]

        val maskedLocal = when {
            local.length <= 1 -> SMALL_MASK
            local.length == 2 -> "${local.first()}$SMALL_MASK"
            else -> "${local.first()}$LARGE_MASK"
        }

        val domainParts = domain.split(".")
        val maskedDomain = if (domainParts.isNotEmpty()) {
            val domainName = domainParts.first()
            val tld = domainParts.drop(1).joinToString(".")

            val maskedDomainName = when {
                domainName.length <= 1 -> SMALL_MASK
                domainName.length == 2 -> "${domainName.first()}$SMALL_MASK"
                else -> "${domainName.first()}$LARGE_MASK"
            }

            if (tld.isNotBlank()) "$maskedDomainName.$tld" else maskedDomainName
        } else {
            LARGE_MASK
        }

        return "$maskedLocal@$maskedDomain"
    }

    /**
     * Masks a phone number by keeping only the last 4 digits.
     *
     * The function strips all non-digit characters before applying the mask.
     * If there are fewer than 4 digits, returns [LARGE_MASK].
     */
    fun maskPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 4) return LARGE_MASK

        val last4 = digits.takeLast(4)
        return "+$LARGE_MASK$last4"
    }

    /**
     * Masks an opaque external identifier by keeping the first 2 characters.
     *
     * If the id is shorter than 4 characters, returns [LARGE_MASK].
     */
    fun maskId(id: String): String {
        if (id.length < 4) return LARGE_MASK
        return id.take(2) + LARGE_MASK
    }

    /**
     * Masks an IP address for logs and audit metadata.
     *
     * - **IPv4:** keeps the first octet, replaces the other three with [SMALL_MASK]
     *   (for example `192.168.1.10` → `192.*.*.*`).
     * - **IPv6:** keeps the first two colon-separated hextets from the normalized host form and
     *   replaces the rest with [LARGE_MASK] (for example `2001:db8::…` → `2001:db8:***` when both
     *   segments appear in the normalized textual form).
     *
     * Leading/trailing whitespace is trimmed. If the value is blank or not a valid IP literal,
     * returns [LARGE_MASK].
     */
    fun maskIpAddress(ip: String): String {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) return LARGE_MASK

        val inet = runCatching { hostLiteralToInetAddress(trimmed) }.getOrNull() ?: return LARGE_MASK

        return when (inet) {
            is Inet4Address -> maskIpv4(inet)
            is Inet6Address -> maskIpv6(inet)
        }
    }

    private fun hostLiteralToInetAddress(host: String): InetAddress {
        val withBracketsStripped = host.removeSurrounding(prefix = "[", suffix = "]")
        val hostOnly = withBracketsStripped.substringBefore('%').trim()
        return InetAddress.getByName(hostOnly)
    }

    private fun maskIpv4(address: Inet4Address): String {
        val host = address.hostAddress ?: return LARGE_MASK
        val octets = host.split('.')
        if (octets.size != 4) return LARGE_MASK
        val first = octets[0]
        return "$first.$SMALL_MASK.$SMALL_MASK.$SMALL_MASK"
    }

    private fun maskIpv6(address: Inet6Address): String {
        val host = address.hostAddress ?: return LARGE_MASK
        val parts = host.split(':').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return LARGE_MASK
        val prefix = if (parts.size >= 2) {
            "${parts[0]}:${parts[1]}"
        } else {
            parts[0]
        }
        return "$prefix:$LARGE_MASK"
    }
}
