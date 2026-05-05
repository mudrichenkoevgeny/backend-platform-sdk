package io.github.mudrichenkoevgeny.backend.core.security.service.otp

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation

/**
 * Data container for a newly generated one-Time Password (OTP) and its configuration.
 *
 * @property otpConfirmation The settings applied to this OTP (TTL, retry period, and length).
 * @property code The actual generated numeric code to be delivered to the user.
 */
data class OtpConfirmationData(
    val otpConfirmation: OtpConfirmation,
    val code: String
)