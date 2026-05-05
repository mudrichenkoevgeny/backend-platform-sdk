package io.github.mudrichenkoevgeny.backend.core.database.manager.redis

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class RedisManagerImplTest {

    private val redisClient = mockk<RedisClient>(relaxed = true)
    private val testScope = TestScope()
    private val connection = mockk<StatefulRedisConnection<String, String>>(relaxed = true)
    private val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
    private val pubSubConnection = mockk<StatefulRedisPubSubConnection<String, String>>(relaxed = true)
    private val pubSubAsyncCommands = mockk<RedisPubSubAsyncCommands<String, String>>(relaxed = true)

    private lateinit var redisManager: RedisManagerImpl

    @BeforeEach
    fun setup() {
        every { redisClient.connect() } returns connection
        every { connection.async() } returns asyncCommands
        every { connection.isOpen } returns true

        every { redisClient.connectPubSub() } returns pubSubConnection
        every { pubSubConnection.async() } returns pubSubAsyncCommands
        every { pubSubConnection.isOpen } returns true

        redisManager = RedisManagerImpl(redisClient, testScope)
    }

    @AfterEach
    fun tearDown() {
        testScope.cancel()
    }

    private fun <T> mockRedisFuture(value: T): RedisFuture<T> {
        val future = object : CompletableFuture<T>(), RedisFuture<T> {
            override fun getError(): String? = null
            override fun await(timeout: Long, unit: TimeUnit): Boolean = true
        }
        future.complete(value)
        return future
    }

    @Test
    fun `incrementWithExpiration executes lua script and returns value`() = runTest {
        val expectedValue: Long = 1L

        every {
            asyncCommands.eval<Long>(
                any<String>(),
                any<ScriptOutputType>(),
                any<Array<String>>(),
                *anyVararg<String>()
            )
        } returns mockRedisFuture(expectedValue)

        val result = redisManager.incrementWithExpiration("key", 10L)

        assertEquals(AppResult.Success(expectedValue), result)
    }

    @Test
    fun `setWithExpiration returns success when command succeeds`() = runTest {
        every { asyncCommands.setex(any<String>(), any<Long>(), any<String>()) } returns mockRedisFuture("OK")

        val result = redisManager.setWithExpiration("key", "value", 10L)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `get returns value when key exists`() = runTest {
        every { asyncCommands.get(any<String>()) } returns mockRedisFuture("some_value")

        val result = redisManager.get("key")

        assertEquals(AppResult.Success("some_value"), result)
    }

    @Test
    fun `exists returns true when count is greater than zero`() = runTest {
        every { asyncCommands.exists(any<String>()) } returns mockRedisFuture(1L)

        val result = redisManager.exists("key")

        assertEquals(AppResult.Success(true), result)
    }

    @Test
    fun `isAvailable returns true on PONG`() = runTest {
        every { asyncCommands.ping() } returns mockRedisFuture("PONG")

        val result = redisManager.isAvailable()

        assertEquals(AppSystemResult.Success(true), result)
    }

    @Test
    fun `shutdown closes connections and client`() = runTest {
        redisManager.warmup()

        val result = redisManager.shutdown()

        assertTrue(result is AppSystemResult.Success)
        verify(exactly = 1) { connection.close() }
        verify(exactly = 1) { pubSubConnection.close() }
        verify(exactly = 1) { redisClient.shutdown() }
    }
}