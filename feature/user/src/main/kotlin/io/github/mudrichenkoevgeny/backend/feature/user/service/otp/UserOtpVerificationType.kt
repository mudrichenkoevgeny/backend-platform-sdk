package io.github.mudrichenkoevgeny.backend.feature.user.service.otp

import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpVerificationType

/**
 * Defines standard OTP verification types used across user authentication and account management flows.
 */
object UserOtpVerificationType {
    /** Confirms email address ownership. */
    val EMAIL_VERIFICATION = OtpVerificationType("email_verification")

    /** Confirms phone number ownership. */
    val PHONE_VERIFICATION = OtpVerificationType("phone_verification")

    /** Used for password recovery flows via email. */
    val EMAIL_PASSWORD_RESET = OtpVerificationType("email_password_reset")
}