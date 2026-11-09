package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiterImpl
import javax.inject.Singleton

/**
 * Dagger module that provides a Redis-backed [RateLimiter].
 *
 * Uses [RedisManager] to store per-action counters with expiration and [AppLogger] to log failures
 * when auxiliary operations (e.g. TTL retrieval) fail.
 */
@Module
public class RateLimiterModule {
    @Provides
    @Singleton
    public fun provideRateLimiter(
        redisManager: RedisManager,
        appLogger: AppLogger
    ): RateLimiter = RateLimiterImpl(
        redisManager = redisManager,
        appLogger = appLogger
    )
}
