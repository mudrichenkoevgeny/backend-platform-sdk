package io.github.mudrichenkoevgeny.backend.core.database.factory.redis

import io.lettuce.core.RedisClient

/**
 * Factory for creating a Lettuce [RedisClient] instance configured from application config.
 */
interface RedisFactory {

    /**
     * Creates and returns a configured [RedisClient].
     *
     * @return connected or connectable [RedisClient] (lifecycle is typically managed by the app or RedisManager).
     */
    fun create(): RedisClient
}