package io.github.mudrichenkoevgeny.backend.core.security.service.otp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Strongly typed OTP verification type used in confirmation flows and API contracts.
 *
 * Defines the purpose of the issued one-time password to prevent cross-usage
 * between different verification scenarios.
 *
 * @property value Raw machine-readable verification type string.
 */
@Serializable
@JvmInline
value class OtpVerificationType(val value: String) {
    init {
        require(value.isNotBlank()) { "OtpVerificationType value must not be blank." }
    }

    override fun toString(): String = value
}

/**
 * Attempts to create an [OtpVerificationType] from this string.
 *
 * Returns `null` if the string is blank.
 */
fun String.toOtpVerificationTypeOrNull(): OtpVerificationType? {
    return if (this.isNotBlank()) {
        OtpVerificationType(this)
    } else {
        null
    }
}

/**
 * Creates an [OtpVerificationType] from this string or throws an exception if the string is blank.
 */
fun String.toOtpVerificationTypeOrThrow(): OtpVerificationType {
    return OtpVerificationType(this)
}