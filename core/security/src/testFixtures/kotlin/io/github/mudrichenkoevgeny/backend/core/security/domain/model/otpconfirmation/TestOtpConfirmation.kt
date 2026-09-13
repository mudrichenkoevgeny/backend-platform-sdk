package io.github.mudrichenkoevgeny.backend.core.security.domain.model.otpconfirmation

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation

fun createTestOtpConfirmation(
    retryAfterSeconds: Int = 60,
    numberOfSymbols: Int = 6,
    expirationSeconds: Int = 300
) = OtpConfirmation(
    retryAfterSeconds = retryAfterSeconds,
    numberOfSymbols = numberOfSymbols,
    expirationSeconds = expirationSeconds
)
