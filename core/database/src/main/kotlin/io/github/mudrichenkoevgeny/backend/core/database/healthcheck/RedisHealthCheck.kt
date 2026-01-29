package io.github.mudrichenkoevgeny.backend.core.database.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckSeverity
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedisHealthCheck @Inject constructor(
    private val redisManager: RedisManager
) : HealthCheck {

    override val severity: HealthCheckSeverity = HealthCheckSeverity.CRITICAL

    override suspend fun check(): AppSystemResult<Unit> {
        return try {
            val isAvailableResult = redisManager.isAvailable()

            val isAvailable = when (isAvailableResult) {
                is AppSystemResult.Success -> isAvailableResult.data
                is AppSystemResult.Error -> return isAvailableResult
            }

            if (isAvailable) {
                AppSystemResult.Success(Unit)
            } else {
                throw RuntimeException("Redis not available")
            }
        } catch (t: Throwable) {
            AppSystemResult.Error(CommonError.System(t))
        }
    }
}