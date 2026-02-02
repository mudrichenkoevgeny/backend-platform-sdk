package io.github.mudrichenkoevgeny.backend.feature.user.service.otp

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType

interface OtpService {
    suspend fun getOtp(identifier: String, type: OtpVerificationType, expirationSeconds: Long = 300): AppResult<String>
    suspend fun getOtpFake(identifier: String): AppResult<String>
    suspend fun verifyOtp(
        identifier: String,
        type: OtpVerificationType,
        code: String,
        deleteOnSuccess: Boolean = true
    ): AppResult<Boolean>
}