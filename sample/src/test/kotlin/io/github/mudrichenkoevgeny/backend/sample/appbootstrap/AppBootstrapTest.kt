package io.github.mudrichenkoevgeny.backend.sample.appbootstrap

import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test

class AppBootstrapTest {

    @Test
    fun `initialize creates database verifies health and warms up redis and telemetry`() {
        val db = mockk<Database>()
        val databaseManager = mockk<DatabaseManager>()
        val redisManager = mockk<RedisManager>()
        val healthCheckerManager = mockk<HealthCheckerManager>()
        val telemetryProvider = mockk<TelemetryProvider>()

        every { databaseManager.create() } returns db
        every { healthCheckerManager.verifyCriticalHealth() } just runs
        every { telemetryProvider.warmup() } just runs
        coEvery { redisManager.warmup() } returns AppSystemResult.Success(Unit)

        val bootstrap = AppBootstrap(
            databaseManager = databaseManager,
            redisManager = redisManager,
            healthCheckManager = healthCheckerManager,
            telemetryProvider = telemetryProvider
        )

        runBlocking {
            bootstrap.initialize()
        }

        coVerifyOrder {
            databaseManager.create()
            healthCheckerManager.verifyCriticalHealth()
            redisManager.warmup()
            telemetryProvider.warmup()
        }
    }
}

