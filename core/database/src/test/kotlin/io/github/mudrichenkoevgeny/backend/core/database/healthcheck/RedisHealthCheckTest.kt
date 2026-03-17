package io.github.mudrichenkoevgeny.backend.core.database.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RedisHealthCheckTest {

    @Test
    fun `check returns Success when Redis is available`() = runBlocking {
        val redisManager = mockk<RedisManager> {
            coEvery { isAvailable() } returns AppSystemResult.Success(true)
        }

        val healthCheck = RedisHealthCheck(redisManager)

        val result = healthCheck.check()

        assertTrue(result is AppSystemResult.Success)
    }

    @Test
    fun `check returns Error when isAvailable returns Error`() = runBlocking {
        val errorResult = AppSystemResult.Error(CommonError.Internal(RuntimeException("redis down")))
        val redisManager = mockk<RedisManager> {
            coEvery { isAvailable() } returns errorResult
        }

        val healthCheck = RedisHealthCheck(redisManager)

        val result = healthCheck.check()

        assertTrue(result is AppSystemResult.Error)
    }

    @Test
    fun `check returns Error when isAvailable throws`() = runBlocking {
        val redisManager = mockk<RedisManager> {
            coEvery { isAvailable() } throws RuntimeException("timeout")
        }

        val healthCheck = RedisHealthCheck(redisManager)

        val result = healthCheck.check()

        assertTrue(result is AppSystemResult.Error)
    }
}
