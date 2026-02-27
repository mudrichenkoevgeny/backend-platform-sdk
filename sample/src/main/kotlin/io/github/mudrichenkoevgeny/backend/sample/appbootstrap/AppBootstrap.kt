package io.github.mudrichenkoevgeny.backend.sample.appbootstrap

import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class AppBootstrap @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val redisManager: RedisManager,
    private val healthCheckManager: HealthCheckerManager,
    private val telemetryProvider: TelemetryProvider
) {
    fun initialize() {
        databaseManager.create()
        healthCheckManager.verifyCriticalHealth()

        runBlocking {
            redisManager.warmup()
            telemetryProvider.warmup()
        }
    }
}