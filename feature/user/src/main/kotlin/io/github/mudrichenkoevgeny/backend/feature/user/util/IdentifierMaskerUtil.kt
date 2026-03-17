package io.github.mudrichenkoevgeny.backend.feature.user.util

/**
 * Utilities for masking user identifiers in logs and audit events.
 *
 * The masking rules are intentionally simple and deterministic:
 * - they keep a small prefix/suffix when possible,
 * - they avoid leaking full identifiers,
 * - they return a fallback mask for malformed inputs.
 */
object IdentifierMaskerUtil {

    const val SMALL_MASK = "*"
    const val LARGE_MASK = "***"

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
     * Masks an external provider identifier by keeping the first 2 characters.
     *
     * If the id is shorter than 4 characters, returns [LARGE_MASK].
     */
    fun maskExternal(externalId: String): String {
        if (externalId.length < 4) return LARGE_MASK
        return externalId.take(2) + LARGE_MASK
    }
}