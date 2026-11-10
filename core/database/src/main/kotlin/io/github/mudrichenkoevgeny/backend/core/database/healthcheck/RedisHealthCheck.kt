package io.github.mudrichenkoevgeny.backend.core.database.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckSeverity
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HealthCheck] implementation that verifies Redis is available via [RedisManager.isAvailable].
 *
 * Returns [AppSystemResult.Success] when Redis responds, [AppSystemResult.Error] on failure.
 * Severity is [HealthCheckSeverity.CRITICAL].
 */
@Singleton
public class RedisHealthCheck @Inject constructor(
    private val redisManager: RedisManager
) : HealthCheck {
    override val severity: HealthCheckSeverity = HealthCheckSeverity.CRITICAL

    override suspend fun check(): AppSystemResult<Unit> = try {
        val isAvailableResult = redisManager.isAvailable()
        when (isAvailableResult) {
            is AppSystemResult.Error -> isAvailableResult
            is AppSystemResult.Success -> {
                if (isAvailableResult.data) {
                    AppSystemResult.Success(Unit)
                } else {
                    AppSystemResult.Error(CommonError.Internal(IllegalStateException("Redis is not available")))
                }
            }
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        AppSystemResult.Error(CommonError.Internal(e))
    }
}
