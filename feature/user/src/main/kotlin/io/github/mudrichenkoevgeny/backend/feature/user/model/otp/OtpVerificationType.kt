package io.github.mudrichenkoevgeny.backend.feature.user.model.otp

/**
 * Purpose of an OTP verification code.
 */
enum class OtpVerificationType {
    /** Confirms that the user owns an email address. */
    EMAIL_VERIFICATION,
    /** Confirms that the user owns a phone number. */
    PHONE_VERIFICATION,
    /** Allows resetting a password using an email-based OTP. */
    EMAIL_PASSWORD_RESET,
    /** Internal-only placeholder type used in test or non-production flows. */
    FAKE
}