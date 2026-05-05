package io.github.mudrichenkoevgeny.backend.core.common.mask

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

object DataMasker {

    const val SMALL_MASK = "*"
    const val LARGE_MASK = "***"

    fun maskPartialValue(value: String): String {
        val v = value.trim()
        if (v.isEmpty()) return v
        return when (v.length) {
            1 -> SMALL_MASK
            2 -> "${v.first()}$SMALL_MASK"
            else -> "${v.first()}$LARGE_MASK${v.last()}"
        }
    }

    fun maskFullValue(value: String): String =
        if (value.isBlank()) value.trim() else LARGE_MASK

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

    fun maskId(id: String): String {
        if (id.length < 4) return LARGE_MASK
        return id.take(2) + LARGE_MASK
    }

    fun maskIpAddress(ip: String): String {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) return LARGE_MASK

        val host = trimmed.removeSurrounding("[", "]").substringBefore('%').trim()

        val inet = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            null
        } ?: return LARGE_MASK

        val hostAddress = inet.hostAddress ?: return LARGE_MASK

        return when (inet) {
            is Inet4Address -> {
                val octets = hostAddress.split('.')
                if (octets.size < 4) LARGE_MASK else "${octets[0]}.$SMALL_MASK.$SMALL_MASK.$SMALL_MASK"
            }
            is Inet6Address -> {
                val parts = hostAddress.split(':').filter { it.isNotEmpty() }
                if (parts.isEmpty()) LARGE_MASK else {
                    val prefix = if (parts.size >= 2) "${parts[0]}:${parts[1]}" else parts[0]
                    "$prefix:$LARGE_MASK"
                }
            }
            else -> LARGE_MASK
        }
    }
}