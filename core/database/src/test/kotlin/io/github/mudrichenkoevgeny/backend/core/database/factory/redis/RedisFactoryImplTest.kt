package io.github.mudrichenkoevgeny.backend.core.database.factory.redis

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.redisclient.RedisClientCreator
import io.lettuce.core.RedisClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RedisFactoryImplTest {

    private companion object {
        private const val REDIS_URL = "redis://localhost:6379"
        private const val TIMEOUT_SECONDS = 5L
    }

    private val config = DatabaseConfig(
        dbUrl = "jdbc:postgresql://localhost/db",
        dbUser = "u",
        dbPassword = "p",
        migrationPaths = emptyList(),
        redisUrl = REDIS_URL,
        redisTimeoutSeconds = TIMEOUT_SECONDS
    )

    @Test
    fun `create returns client from RedisClientCreator with config url and timeout`() {
        val redisClient = RedisClient.create(REDIS_URL)
        val redisClientCreator = mockk<RedisClientCreator> {
            every { create(REDIS_URL, TIMEOUT_SECONDS) } returns redisClient
        }
        val appLogger = mockk<AppLogger>(relaxUnitFun = true)

        val factory = RedisFactoryImpl(
            redisClientCreator = redisClientCreator,
            appLogger = appLogger,
            databaseConfig = config
        )

        val result = factory.create()

        assertSame(redisClient, result)
        verify(exactly = 1) { redisClientCreator.create(REDIS_URL, TIMEOUT_SECONDS) }
        redisClient.shutdown()
    }

    @Test
    fun `create logs and rethrows when RedisClientCreator throws`() {
        val redisClientCreator = mockk<RedisClientCreator> {
            every { create(any(), any()) } throws RuntimeException("connect failed")
        }
        val appLogger = mockk<AppLogger>(relaxUnitFun = true)

        val factory = RedisFactoryImpl(
            redisClientCreator = redisClientCreator,
            appLogger = appLogger,
            databaseConfig = config
        )

        assertThrows<RuntimeException> { factory.create() }
        verify(exactly = 1) { appLogger.logError(any()) }
    }
}
