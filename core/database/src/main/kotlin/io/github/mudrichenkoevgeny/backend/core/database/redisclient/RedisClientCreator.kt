package io.github.mudrichenkoevgeny.backend.core.database.redisclient

import io.lettuce.core.RedisClient

/**
 * Creates a Lettuce [RedisClient] from a URL and connection timeout.
 */
interface RedisClientCreator {

    /**
     * Builds a [RedisClient] for the given Redis URL and timeout.
     *
     * @param url Redis connection URL (e.g. `redis://localhost:6379`).
     * @param timeoutSeconds connection timeout in seconds.
     * @return configured [RedisClient] (caller is responsible for lifecycle).
     */
    fun create(url: String, timeoutSeconds: Long): RedisClient
}