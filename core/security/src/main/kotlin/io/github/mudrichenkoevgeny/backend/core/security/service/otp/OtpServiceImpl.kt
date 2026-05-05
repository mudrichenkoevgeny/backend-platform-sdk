package io.github.mudrichenkoevgeny.backend.core.security.service.otp

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Redis-backed [OtpService] implementation.
 *
 * Manages one-time passwords by storing them as plain strings in Redis. Keys are deterministically
 * generated based on the [OtpVerificationType] and a unique user identifier.
 *
 * Key features:
 * - Prevents brute-force/spam by enforcing a cooling-off period ([OtpConfirmation.retryAfterSeconds]).
 * - Reuses existing valid codes if the cooling-off period has passed but the code hasn't expired.
 * - Supports automatic expiration via Redis TTL.
 * - Provides optional deletion upon successful verification.
 */
@Singleton
class OtpServiceImpl @Inject constructor(
    private val redisManager: RedisManager,
    private val securitySettingsProvider: SecuritySettingsProvider
) : OtpService {

    override suspend fun getOtp(
        identifier: String,
        type: OtpVerificationType
    ): AppResult<OtpConfirmationData> {
        val otpConfirmation = securitySettingsProvider.getOtpConfirmation()
        val key = buildKey(identifier, type)

        val savedCodeResult = redisManager.get(key)
        val savedCode = when (savedCodeResult) {
            is AppResult.Success -> savedCodeResult.data
            is AppResult.Error -> return savedCodeResult
        }

        if (savedCode != null) {
            val ttlResult = redisManager.getTtl(key)
            val ttl = when (ttlResult) {
                is AppResult.Success -> ttlResult.data
                is AppResult.Error -> return ttlResult
            }

            val elapsedSeconds = otpConfirmation.expirationSeconds - ttl

            if (elapsedSeconds < otpConfirmation.retryAfterSeconds) {
                val remainingWait = otpConfirmation.retryAfterSeconds - elapsedSeconds.toInt()
                return AppResult.Error(SecurityError.OtpRetryTooSoon(remainingWait))
            }

            return AppResult.Success(
                OtpConfirmationData(
                    otpConfirmation = otpConfirmation,
                    code = savedCode
                )
            )
        }

        val start = 10.0.pow(otpConfirmation.numberOfSymbols - 1).toInt()
        val end = 10.0.pow(otpConfirmation.numberOfSymbols).toInt() - 1
        val code = (start..end).random().toString()

        return redisManager.setWithExpiration(
            key = key,
            value = code,
            expirationSeconds = otpConfirmation.expirationSeconds.toLong()
        ).mapSuccess {
            OtpConfirmationData(
                otpConfirmation = otpConfirmation,
                code = code
            )
        }
    }

    override suspend fun verifyOtp(
        identifier: String,
        type: OtpVerificationType,
        code: String,
        deleteOnSuccess: Boolean
    ): AppResult<Boolean> {
        val key = buildKey(identifier, type)
        val savedCodeResult = redisManager.get(key)

        val savedCode = when (savedCodeResult) {
            is AppResult.Success -> savedCodeResult.data
            is AppResult.Error -> return savedCodeResult
        }

        if (savedCode == null || savedCode != code) {
            return AppResult.Success(false)
        }

        if (deleteOnSuccess) {
            redisManager.delete(key)
        }

        return AppResult.Success(true)
    }

    private fun buildKey(identifier: String, type: OtpVerificationType): String {
        return "otp:${type.value.lowercase()}:$identifier"
    }
}