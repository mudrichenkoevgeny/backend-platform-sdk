package io.github.mudrichenkoevgeny.backend.core.database.manager.redis

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedisManagerImpl @Inject constructor(
    private val redisClient: RedisClient
) : RedisManager {

    @Volatile
    private var connection: StatefulRedisConnection<String, String>? = null
    private val mutex = Mutex()

    private val incrementScript = """
        local current = redis.call('INCR', KEYS[1])
        if current == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        return current
    """.trimIndent()

    override suspend fun setWithExpiration(key: String, value: String, expirationSeconds: Long): AppResult<Unit> {
        return try {
            getConnection().async().setex(key, expirationSeconds, value).await()
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override suspend fun incrementWithExpiration(key: String, expirationSeconds: Long): AppResult<Long> {
        return try {
            val incrementResult = getConnection().async().eval<Long>(
                incrementScript,
                ScriptOutputType.INTEGER,
                arrayOf(key),
                expirationSeconds.toString()
            ).await()
            AppResult.Success(incrementResult)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override suspend fun get(key: String): AppResult<String?> {
        return try {
            val value = getConnection().async().get(key).await()
            AppResult.Success(value)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override suspend fun getTtl(key: String): AppResult<Long> {
        return try {
            val ttl = getConnection().async().ttl(key).await()
            AppResult.Success(ttl)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override suspend fun exists(key: String): AppResult<Boolean> {
        return try {
            val exist = getConnection().async().exists(key).await() > 0
            AppResult.Success(exist)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override suspend fun delete(key: String): AppResult<Unit> {
        return try {
            getConnection().async().del(key).await()
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override suspend fun isAvailable(): AppSystemResult<Boolean> {
        return try {
            val response = getConnection().async().ping().await()
            val isAvailable = response == PING_RESPONSE
            AppSystemResult.Success(isAvailable)
        } catch (t: Throwable) {
            AppSystemResult.Error(CommonError.System(t))
        }
    }

    override suspend fun warmup(): AppSystemResult<Unit> {
        return try {
            getConnection()
            AppSystemResult.Success(Unit)
        } catch (t: Throwable) {
            AppSystemResult.Error(CommonError.System(t))
        }
    }

    override fun shutdown(): AppSystemResult<Unit> {
        return try {
            connection?.close()
            redisClient.shutdown()
            AppSystemResult.Success(Unit)
        } catch (t: Throwable) {
            AppSystemResult.Error(CommonError.System(t))
        }
    }

    private suspend fun getConnection(): StatefulRedisConnection<String, String> {
        val current = connection
        if (current != null && current.isOpen) {
            return current
        }

        return mutex.withLock {
            val doubleCheck = connection
            if (doubleCheck != null && doubleCheck.isOpen) {
                doubleCheck
            } else {
                connection?.close()
                val newConn = redisClient.connect()
                connection = newConn
                newConn
            }
        }
    }

    companion object {
        private const val PING_RESPONSE = "PONG"
    }
}