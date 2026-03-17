package io.github.mudrichenkoevgeny.backend.feature.user.service.otp

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType

/**
 * Issues and verifies one-time passwords (OTPs) for a specific identifier and verification type.
 *
 * Typical usage:
 * - request an OTP for an identifier ([getOtp]) and deliver it through an out-of-band channel;
 * - verify the submitted code ([verifyOtp]) with optional deletion on success.
 */
interface OtpService {
    /**
     * Returns an OTP code for [identifier] and [type].
     *
     * Implementations may reuse an existing non-expired code or generate a new one.
     *
     * @param expirationSeconds desired TTL for newly generated codes
     */
    suspend fun getOtp(identifier: String, type: OtpVerificationType, expirationSeconds: Long = 300): AppResult<String>

    /**
     * Convenience wrapper for issuing a short-lived fake OTP.
     */
    suspend fun getOtpFake(identifier: String): AppResult<String>

    /**
     * Verifies [code] against the stored OTP for [identifier] and [type].
     *
     * @param deleteOnSuccess whether to delete the stored OTP when verification succeeds
     * @return [AppResult.Success] with `true` when code matches; `false` when missing/mismatch
     */
    suspend fun verifyOtp(
        identifier: String,
        type: OtpVerificationType,
        code: String,
        deleteOnSuccess: Boolean = true
    ): AppResult<Boolean>
}