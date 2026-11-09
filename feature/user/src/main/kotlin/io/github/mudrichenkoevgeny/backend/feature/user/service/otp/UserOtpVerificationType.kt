package io.github.mudrichenkoevgeny.backend.feature.user.service.otp

import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpVerificationType

/**
 * Defines standard OTP verification types used across user authentication and account management flows.
 */
public object UserOtpVerificationType {
    /** Confirms email address ownership. */
    public val EMAIL_VERIFICATION: OtpVerificationType = OtpVerificationType("email_verification")

    /** Confirms phone number ownership. */
    public val PHONE_VERIFICATION: OtpVerificationType = OtpVerificationType("phone_verification")

    /** Used for password recovery flows via email. */
    public val EMAIL_PASSWORD_RESET: OtpVerificationType = OtpVerificationType("email_password_reset")

    /** Used for account unlock flows via email. */
    public val EMAIL_UNLOCK: OtpVerificationType = OtpVerificationType("email_unlock")

    /** Used for account unlock flows via phone. */
    public val PHONE_UNLOCK: OtpVerificationType = OtpVerificationType("phone_unlock")
}
