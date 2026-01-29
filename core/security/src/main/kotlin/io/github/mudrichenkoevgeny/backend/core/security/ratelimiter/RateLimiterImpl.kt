package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimiterImpl @Inject constructor(
    private val redisManager: RedisManager,
    private val appLogger: AppLogger
) : RateLimiter {
    override suspend fun isRateLimited(
        action: RateLimitAction,
        identifier: String
    ): AppResult<RateLimitResult> {
        val key = action.createKey(identifier)

        val currentCountResult = redisManager.incrementWithExpiration(
            key = key,
            expirationSeconds = action.windowSeconds.toLong()
        )

        val currentCount = when (currentCountResult) {
            is AppResult.Success -> currentCountResult.data
            is AppResult.Error -> return currentCountResult
        }

        if (currentCount > action.limit) {
            val ttlResult = redisManager.getTtl(key)

            val ttl = when (ttlResult) {
                is AppResult.Success -> ttlResult.data
                is AppResult.Error -> {
                    appLogger.logError(ttlResult.error)
                    action.windowSeconds.toLong()
                }
            }

            return AppResult.Success(
                RateLimitResult.Exceeded(
                    CommonError.TooManyRequests(
                        rateLimitActionCode = action.id,
                        limit = action.limit,
                        identifier = key,
                        retryAfterSeconds = if (ttl > 0) {
                            ttl.toInt()
                        } else {
                            action.windowSeconds
                        }
                    )
                )
            )
        }

        return AppResult.Success(RateLimitResult.Allowed)
    }
}