package com.mudrichenkoevgeny.backend.feature.user.util

object IdentifierMaskerUtil {

    const val SMALL_MASK = "*"
    const val LARGE_MASK = "***"

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

    fun maskPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 4) return LARGE_MASK

        val last4 = digits.takeLast(4)
        return "+$LARGE_MASK$last4"
    }

    fun maskExternal(externalId: String): String {
        if (externalId.length < 4) return LARGE_MASK
        return externalId.take(2) + LARGE_MASK
    }
}