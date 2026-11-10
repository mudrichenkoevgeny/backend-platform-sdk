package io.github.mudrichenkoevgeny.backend.core.database.factory.redis

import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.redisclient.RedisClientCreator
import io.lettuce.core.RedisClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [RedisFactory] implementation that delegates to [RedisClientCreator] with [DatabaseConfig.redisUrl]
 * and [DatabaseConfig.redisTimeoutSeconds].
 */
@Singleton
public class RedisFactoryImpl @Inject constructor(
    private val redisClientCreator: RedisClientCreator,
    private val databaseConfig: DatabaseConfig
) : RedisFactory {
    override fun create(): RedisClient = redisClientCreator.create(
        url = databaseConfig.redisUrl,
        timeoutSeconds = databaseConfig.redisTimeoutSeconds
    )
}
