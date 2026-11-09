package io.github.mudrichenkoevgeny.backend.core.settings.integration

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManagerImpl
import io.github.mudrichenkoevgeny.backend.core.database.redisclient.LettuceRedisClientCreator
import io.github.mudrichenkoevgeny.backend.core.settings.manager.SystemSettingsManager
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsServiceImpl
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RequiresDocker
internal class RedisSettingsIntegrationTest {
    private class InMemorySystemSettingsManager : SystemSettingsManager {
        private val storage = ConcurrentHashMap<String, SystemSetting>()

        override suspend fun saveSettings(settings: List<SystemSetting>): AppResult<List<SystemSetting>> {
            settings.forEach { storage[it.key] = it }
            return AppResult.Success(settings)
        }

        override suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting> {
            storage[setting.key] = setting
            return AppResult.Success(setting)
        }

        override suspend fun getSettingByKey(key: String): AppResult<SystemSetting?> =
            AppResult.Success(storage[key])

        override suspend fun getAllSettings(): AppResult<List<SystemSetting>> =
            AppResult.Success(storage.values.toList())

        override suspend fun deleteSetting(key: String): AppResult<Unit> {
            storage.remove(key)
            return AppResult.Success(Unit)
        }
    }

    @Test
    fun `redis manager should perform basic key-value and ttl operations`() {
        GenericContainer(DockerImageName.parse(REDIS_IMAGE)).withExposedPorts(REDIS_PORT).use { redis ->
            redis.start()
            assertTrue(redis.isRunning)

            val redisUrl = "redis://${redis.host}:${redis.getMappedPort(REDIS_PORT)}"
            val appLogger = mockk<AppLogger>(relaxed = true)
            val clientCreator = LettuceRedisClientCreator(appLogger)
            val redisClient = clientCreator.create(
                url = redisUrl,
                timeoutSeconds = TIMEOUT_SECONDS
            )
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val redisManager = RedisManagerImpl(
                redisClient = redisClient,
                scope = scope
            )

            try {
                runBlocking {
                    val setResult = redisManager.setWithExpiration(
                        key = TEST_KEY,
                        value = TEST_VALUE,
                        expirationSeconds = 60
                    )
                    assertTrue(setResult is AppResult.Success)

                    val existsResult = redisManager.exists(TEST_KEY)
                    assertTrue((existsResult is AppResult.Success) && existsResult.data)

                    val getResult = redisManager.get(TEST_KEY)
                    assertTrue((getResult is AppResult.Success) && (getResult.data == TEST_VALUE))

                    val ttlResult = redisManager.getTtl(TEST_KEY)
                    assertTrue((ttlResult is AppResult.Success) && (ttlResult.data > 0))

                    val deleteResult = redisManager.delete(TEST_KEY)
                    assertTrue(deleteResult is AppResult.Success)

                    val existsAfterDelete = redisManager.exists(TEST_KEY)
                    assertTrue((existsAfterDelete is AppResult.Success) && !existsAfterDelete.data)
                }
            } finally {
                redisManager.shutdown()
                scope.cancel()
            }
        }
    }

    @Test
    fun `redis pub sub should synchronize setting cache invalidation between two nodes`() {
        GenericContainer(DockerImageName.parse(REDIS_IMAGE)).withExposedPorts(REDIS_PORT).use { redis ->
            redis.start()
            assertTrue(redis.isRunning)

            val redisUrl = "redis://${redis.host}:${redis.getMappedPort(REDIS_PORT)}"
            val appLogger = mockk<AppLogger>(relaxed = true)
            val clientCreator = LettuceRedisClientCreator(appLogger)

            val sharedManager = InMemorySystemSettingsManager()
            runBlocking {
                sharedManager.saveSetting(
                    SystemSetting(
                        key = SETTING_KEY,
                        value = INITIAL_VALUE,
                        type = SettingType.STRING
                    )
                )
            }

            val node1Scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val node2Scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            val redisClient1 = clientCreator.create(
                url = redisUrl,
                timeoutSeconds = TIMEOUT_SECONDS
            )
            val redisClient2 = clientCreator.create(
                url = redisUrl,
                timeoutSeconds = TIMEOUT_SECONDS
            )

            val redisManager1 = RedisManagerImpl(
                redisClient = redisClient1,
                scope = node1Scope
            )
            val redisManager2 = RedisManagerImpl(
                redisClient = redisClient2,
                scope = node2Scope
            )

            val service1 = SystemSettingsServiceImpl(
                systemSettingsManager = sharedManager,
                redisManager = redisManager1,
                scope = node1Scope
            )
            val service2 = SystemSettingsServiceImpl(
                systemSettingsManager = sharedManager,
                redisManager = redisManager2,
                scope = node2Scope
            )

            try {
                runBlocking {
                    service1.initialize()
                    service2.initialize()

                    assertEquals(INITIAL_VALUE, service1.getString(SETTING_KEY))
                    assertEquals(INITIAL_VALUE, service2.getString(SETTING_KEY))

                    val updateResult = service1.updateSetting(
                        key = SETTING_KEY,
                        value = UPDATED_VALUE,
                        type = SettingType.STRING
                    )
                    assertTrue(updateResult is AppResult.Success)
                    assertEquals(UPDATED_VALUE, service1.getString(SETTING_KEY))

                    withTimeout(TIMEOUT_SECONDS.seconds) {
                        while (service2.getString(SETTING_KEY) != UPDATED_VALUE) {
                            delay(100.milliseconds)
                        }
                    }

                    assertEquals(UPDATED_VALUE, service2.getString(SETTING_KEY))
                }
            } finally {
                redisManager1.shutdown()
                redisManager2.shutdown()
                node1Scope.cancel()
                node2Scope.cancel()
            }
        }
    }

    private companion object {
        private const val REDIS_IMAGE = "redis:7-alpine"
        private const val REDIS_PORT = 6379
        private const val TEST_KEY = "test.redis.key"
        private const val TEST_VALUE = "test.redis.value"
        private const val SETTING_KEY = "global.contact_support_email"
        private const val INITIAL_VALUE = "support-old@example.com"
        private const val UPDATED_VALUE = "support-new@example.com"
        private const val TIMEOUT_SECONDS = 5L
    }
}
