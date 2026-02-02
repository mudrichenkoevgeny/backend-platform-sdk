package io.github.mudrichenkoevgeny.backend.feature.user.service.otp

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtpServiceImpl @Inject constructor(
    private val redisManager: RedisManager
) : OtpService {

    override suspend fun getOtp(
        identifier: String,
        type: OtpVerificationType,
        expirationSeconds: Long
    ): AppResult<String> {
        val key = buildKey(identifier, type)
        val savedCodeResult = redisManager.get(key)

        val savedCode = when (savedCodeResult) {
            is AppResult.Success -> savedCodeResult.data
            is AppResult.Error -> return savedCodeResult
        }

        if (savedCode != null) {
            return AppResult.Success(savedCode)
        }

        val code = (100000..999999).random().toString()

        redisManager.setWithExpiration(key, code, expirationSeconds)

        return AppResult.Success(code)
    }

    override suspend fun getOtpFake(identifier: String): AppResult<String> {
        return getOtp(
            identifier = identifier,
            type = OtpVerificationType.FAKE,
            expirationSeconds = 1
        )
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
        return "otp:${type.name.lowercase()}:$identifier"
    }
}