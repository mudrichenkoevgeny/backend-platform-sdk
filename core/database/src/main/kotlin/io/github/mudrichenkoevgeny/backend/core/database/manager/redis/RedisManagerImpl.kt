package io.github.mudrichenkoevgeny.backend.core.database.manager.redis

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [RedisManager] implementation backed by a Lettuce [RedisClient].
 *
 * Lazily obtains a single [StatefulRedisConnection] (thread-safe via mutex), runs commands asynchronously with [await],
 * and wraps failures in [AppResult.Error] or [AppSystemResult.Error] with [CommonError.Internal].
 * [shutdown] closes the connection and the client.
 */
@Singleton
class RedisManagerImpl @Inject constructor(
    private val redisClient: RedisClient,
    @param:BackgroundScope private val scope: CoroutineScope
) : RedisManager {

    @Volatile
    private var connection: StatefulRedisConnection<String, String>? = null

    @Volatile
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>? = null

    private val mutex = Mutex()
    private val pubSubLock = Any()

    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<suspend (String) -> Unit>>()

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
            AppResult.Error(CommonError.Internal(t))
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
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override suspend fun get(key: String): AppResult<String?> {
        return try {
            val value = getConnection().async().get(key).await()
            AppResult.Success(value)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override suspend fun getTtl(key: String): AppResult<Long> {
        return try {
            val ttl = getConnection().async().ttl(key).await()
            AppResult.Success(ttl)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override suspend fun exists(key: String): AppResult<Boolean> {
        return try {
            val exist = getConnection().async().exists(key).await() > 0
            AppResult.Success(exist)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override suspend fun delete(key: String): AppResult<Unit> {
        return try {
            getConnection().async().del(key).await()
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override suspend fun publish(channel: String, message: String): AppResult<Unit> {
        return try {
            getConnection().async().publish(channel, message).await()
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override fun subscribe(channel: String, onMessage: suspend (message: String) -> Unit) {
        synchronized(pubSubLock) {
            val conn = getPubSubConnection()

            val channelListeners = listeners.getOrPut(channel) { CopyOnWriteArrayList() }
            channelListeners.add(onMessage)

            conn.async().subscribe(channel)
        }
    }

    private fun getPubSubConnection(): StatefulRedisPubSubConnection<String, String> {
        val current = pubSubConnection
        if (current != null && current.isOpen) return current

        return synchronized(pubSubLock) {
            val doubleCheck = pubSubConnection
            if (doubleCheck != null && doubleCheck.isOpen) {
                doubleCheck
            } else {
                pubSubConnection?.close()
                val newConn = redisClient.connectPubSub()

                newConn.addListener(object : RedisPubSubAdapter<String, String>() {
                    override fun message(ch: String, msg: String) {
                        listeners[ch]?.forEach { callback ->
                            scope.launch { callback(msg) }
                        }
                    }
                })

                pubSubConnection = newConn
                newConn
            }
        }
    }

    override suspend fun isAvailable(): AppSystemResult<Boolean> {
        return try {
            val response = getConnection().async().ping().await()
            val isAvailable = response == PING_RESPONSE
            AppSystemResult.Success(isAvailable)
        } catch (t: Throwable) {
            AppSystemResult.Error(CommonError.Internal(t))
        }
    }

    override suspend fun warmup(): AppSystemResult<Unit> {
        return try {
            getConnection()
            getPubSubConnection()
            AppSystemResult.Success(Unit)
        } catch (t: Throwable) {
            AppSystemResult.Error(CommonError.Internal(t))
        }
    }

    override fun shutdown(): AppSystemResult<Unit> {
        return try {
            connection?.close()
            pubSubConnection?.close()
            redisClient.shutdown()
            AppSystemResult.Success(Unit)
        } catch (t: Throwable) {
            AppSystemResult.Error(CommonError.Internal(t))
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