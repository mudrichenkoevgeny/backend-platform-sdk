package io.github.mudrichenkoevgeny.backend.core.database.di.module

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.factory.redis.RedisFactory
import io.github.mudrichenkoevgeny.backend.core.database.factory.redis.RedisFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManagerImpl
import io.github.mudrichenkoevgeny.backend.core.database.redisclient.LettuceRedisClientCreator
import io.github.mudrichenkoevgeny.backend.core.database.redisclient.RedisClientCreator
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
interface RedisModule {

    @Binds
    @Singleton
    fun bindRedisClientCreator(lettuceRedisClientCreator: LettuceRedisClientCreator): RedisClientCreator

    companion object {
        @Provides
        @Singleton
        fun provideRedisFactory(
            redisClientCreator: RedisClientCreator,
            appLogger: AppLogger,
            databaseConfig: DatabaseConfig
        ): RedisFactory {
            return RedisFactoryImpl(
                redisClientCreator = redisClientCreator,
                appLogger = appLogger,
                databaseConfig = databaseConfig
            )
        }

        @Provides
        @Singleton
        fun provideRedisManager(
            factory: RedisFactory
        ): RedisManager {
            val redisClient = factory.create()
            return RedisManagerImpl(
                redisClient = redisClient
            )
        }
    }
}