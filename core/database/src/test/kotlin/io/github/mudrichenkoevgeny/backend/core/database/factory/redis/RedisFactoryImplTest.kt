package io.github.mudrichenkoevgeny.backend.core.database.factory.redis

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
            every {
                create(
                    REDIS_URL,
                    TIMEOUT_SECONDS
                )
            } returns redisClient
        }

        val factory = RedisFactoryImpl(
            redisClientCreator = redisClientCreator,
            databaseConfig = config
        )

        val result = factory.create()

        assertSame(
            redisClient,
            result
        )
        verify(exactly = 1) {
            redisClientCreator.create(
                REDIS_URL,
                TIMEOUT_SECONDS
            )
        }
        redisClient.shutdown()
    }

    @Test
    fun `create logs and rethrows when RedisClientCreator throws`() {
        val redisClientCreator = mockk<RedisClientCreator> {
            every {
                create(
                    any(),
                    any()
                )
            } throws IllegalArgumentException("connect failed")
        }

        val factory = RedisFactoryImpl(
            redisClientCreator = redisClientCreator,
            databaseConfig = config
        )

        assertThrows<IllegalArgumentException> { factory.create() }
    }

    private companion object {
        private const val REDIS_URL = "redis://localhost:6379"
        private const val TIMEOUT_SECONDS = 5L
    }
}
