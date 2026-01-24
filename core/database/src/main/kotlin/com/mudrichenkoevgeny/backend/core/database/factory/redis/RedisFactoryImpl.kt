package com.mudrichenkoevgeny.backend.core.database.factory.redis

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import com.mudrichenkoevgeny.backend.core.database.redisclient.RedisClientCreator
import io.lettuce.core.RedisClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedisFactoryImpl @Inject constructor(
    private val redisClientCreator: RedisClientCreator,
    private val appLogger: AppLogger,
    private val databaseConfig: DatabaseConfig
): RedisFactory {

    override fun create(): RedisClient {
        return try {
            redisClientCreator.create(
                url = databaseConfig.redisUrl,
                timeoutSeconds = databaseConfig.redisTimeoutSeconds
            )
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            throw t
        }
    }
}