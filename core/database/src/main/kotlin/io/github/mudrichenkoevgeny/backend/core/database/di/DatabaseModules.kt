package io.github.mudrichenkoevgeny.backend.core.database.di

import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.di.module.DatabaseConfigModule
import io.github.mudrichenkoevgeny.backend.core.database.di.module.DatabaseHealthCheckModule
import io.github.mudrichenkoevgeny.backend.core.database.di.module.DatabaseModule
import io.github.mudrichenkoevgeny.backend.core.database.di.module.RedisModule
import io.github.mudrichenkoevgeny.backend.core.database.factory.redis.RedisFactory
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import dagger.Module

/**
 * Aggregate Dagger module for the database feature.
 *
 * Includes [DatabaseConfigModule] (config factory and [DatabaseConfig]),
 * [DatabaseModule] (DataSource, [DatabaseManager], Exposed Database), [RedisModule] ([RedisManager], [RedisFactory]),
 * and [DatabaseHealthCheckModule] (DB and Redis health checks into the [HealthCheck] set).
 */
@Module(
    includes = [
        DatabaseConfigModule::class,
        DatabaseModule::class,
        RedisModule::class,
        DatabaseHealthCheckModule::class
    ]
)
interface DatabaseModules