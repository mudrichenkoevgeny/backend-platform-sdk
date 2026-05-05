package io.github.mudrichenkoevgeny.backend.core.database.manager.redis

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult

/**
 * Redis operations and lifecycle: key-value commands with optional TTL, availability check, warmup, and shutdown.
 * All suspend functions use the same connection; errors are wrapped in [AppResult] or [AppSystemResult].
 */
interface RedisManager {

    /**
     * Sets a string [value] for the [key] with a specified [expirationSeconds].
     * Uses the `SETEX` command.
     */
    suspend fun setWithExpiration(key: String, value: String, expirationSeconds: Long): AppResult<Unit>

    /**
     * Increments the number stored at [key] and sets [expirationSeconds] if the key is new.
     * Uses an atomic Lua script to ensure TTL is set only on the first increment (when value becomes 1).
     * @return [AppResult.Success] with the value after increment.
     */
    suspend fun incrementWithExpiration(key: String, expirationSeconds: Long): AppResult<Long>

    /**
     * Retrieves the string value of [key].
     * @return [AppResult.Success] with the value, or `null` if the key does not exist.
     */
    suspend fun get(key: String): AppResult<String?>

    /**
     * Returns the remaining time to live of a [key] that has a timeout.
     * @return [AppResult.Success] with TTL in seconds, or a negative value if key has no TTL or doesn't exist.
     */
    suspend fun getTtl(key: String): AppResult<Long>

    /**
     * Checks if the specified [key] exists in the storage.
     * @return [AppResult.Success] with `true` if exists, `false` otherwise.
     */
    suspend fun exists(key: String): AppResult<Boolean>

    /**
     * Removes the specified [key].
     */
    suspend fun delete(key: String): AppResult<Unit>

    /**
     * Posts a [message] to the given [channel].
     */
    suspend fun publish(channel: String, message: String): AppResult<Unit>

    /**
     * Subscribes to the specified [channel].
     * The [onMessage] block is executed in the manager's background scope whenever a message is received.
     * Multiple listeners can be attached to the same channel.
     */
    fun subscribe(channel: String, onMessage: suspend (message: String) -> Unit)

    /**
     * Performs a health check by sending a `PING` command.
     * @return [AppSystemResult.Success] with `true` if Redis responds with `PONG`.
     */
    suspend fun isAvailable(): AppSystemResult<Boolean>

    /**
     * Forces initialization of standard and Pub/Sub connections.
     * Useful for early failure detection during application startup.
     */
    suspend fun warmup(): AppSystemResult<Unit>

    /**
     * Closes active connections and releases Redis client resources.
     * Should be called during application shutdown.
     */
    fun shutdown(): AppSystemResult<Unit>
}