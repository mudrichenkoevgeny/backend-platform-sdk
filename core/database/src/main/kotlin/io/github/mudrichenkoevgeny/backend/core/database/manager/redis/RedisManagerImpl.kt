package io.github.mudrichenkoevgeny.backend.core.database.manager.redis

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisException
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
import kotlin.coroutines.cancellation.CancellationException

/**
 * Default [RedisManager] implementation backed by a Lettuce [RedisClient].
 *
 * Lazily obtains a single [StatefulRedisConnection] (thread-safe via mutex), runs commands asynchronously with [await],
 * and wraps failures in [AppResult.Error] or [AppSystemResult.Error] with [CommonError.Internal].
 * [shutdown] closes the connection and the client.
 */
@Singleton
public class RedisManagerImpl @Inject constructor(
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

    private val incrementScript =
        """
        local current = redis.call('INCR', KEYS[1])
        if current == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        return current
        """.trimIndent()

    override suspend fun setWithExpiration(
        key: String,
        value: String,
        expirationSeconds: Long
    ): AppResult<Unit> = execute {
        getConnection()
            .async()
            .setex(
                key,
                expirationSeconds,
                value
            ).await()
    }

    override suspend fun incrementWithExpiration(
        key: String,
        expirationSeconds: Long
    ): AppResult<Long> = execute {
        getConnection()
            .async()
            .eval<Long>(
                incrementScript,
                ScriptOutputType.INTEGER,
                arrayOf(key),
                expirationSeconds.toString()
            ).await()
    }

    override suspend fun get(key: String): AppResult<String?> = execute {
        getConnection().async().get(key).await()
    }

    override suspend fun getTtl(key: String): AppResult<Long> = execute {
        getConnection().async().ttl(key).await()
    }

    override suspend fun exists(key: String): AppResult<Boolean> = execute {
        getConnection().async().exists(key).await() > 0
    }

    override suspend fun delete(key: String): AppResult<Unit> = execute {
        getConnection().async().del(key).await()
    }

    override suspend fun deleteBatch(keys: List<String>): AppResult<Unit> {
        if (keys.isEmpty()) {
            return AppResult.Success(Unit)
        }
        return execute {
            getConnection().async().del(*keys.toTypedArray()).await()
        }
    }

    override suspend fun publish(
        channel: String,
        message: String
    ): AppResult<Unit> = execute {
        getConnection()
            .async()
            .publish(
                channel,
                message
            ).await()
    }

    override fun subscribe(
        channel: String,
        onMessage: suspend (message: String) -> Unit
    ) {
        synchronized(pubSubLock) {
            val conn = getPubSubConnection()

            val channelListeners = listeners.getOrPut(channel) { CopyOnWriteArrayList() }
            channelListeners.add(onMessage)

            conn.async().subscribe(channel)
        }
    }

    private fun getPubSubConnection(): StatefulRedisPubSubConnection<String, String> {
        val current = pubSubConnection
        if (current != null && current.isOpen) {
            return current
        }

        return synchronized(pubSubLock) {
            val doubleCheck = pubSubConnection
            if (doubleCheck != null && doubleCheck.isOpen) {
                doubleCheck
            } else {
                pubSubConnection?.close()
                val newConn = redisClient.connectPubSub()

                newConn.addListener(object : RedisPubSubAdapter<String, String>() {
                    override fun message(
                        ch: String,
                        msg: String
                    ) {
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

    override suspend fun isAvailable(): AppSystemResult<Boolean> = executeSystem {
        val response = getConnection().async().ping().await()
        response == PING_RESPONSE
    }

    override suspend fun warmup(): AppSystemResult<Unit> = executeSystem {
        getConnection()
        getPubSubConnection()
    }

    override fun shutdown(): AppSystemResult<Unit> = try {
        connection?.close()
        pubSubConnection?.close()
        redisClient.shutdown()
        AppSystemResult.Success(Unit)
    } catch (e: RedisException) {
        AppSystemResult.Error(CommonError.Internal(e))
    } catch (e: IllegalStateException) {
        AppSystemResult.Error(CommonError.Internal(e))
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

    private suspend fun <T> execute(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: RedisException) {
        AppResult.Error(CommonError.Internal(e))
    } catch (e: IllegalArgumentException) {
        AppResult.Error(CommonError.Internal(e))
    } catch (e: IllegalStateException) {
        AppResult.Error(CommonError.Internal(e))
    }

    private suspend fun <T> executeSystem(block: suspend () -> T): AppSystemResult<T> = try {
        AppSystemResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: RedisException) {
        AppSystemResult.Error(CommonError.Internal(e))
    } catch (e: IllegalArgumentException) {
        AppSystemResult.Error(CommonError.Internal(e))
    } catch (e: IllegalStateException) {
        AppSystemResult.Error(CommonError.Internal(e))
    }

    public companion object {
        private const val PING_RESPONSE = "PONG"
    }
}
