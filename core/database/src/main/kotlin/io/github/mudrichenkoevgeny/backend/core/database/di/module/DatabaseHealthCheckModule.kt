package io.github.mudrichenkoevgeny.backend.core.database.di.module

import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import io.github.mudrichenkoevgeny.backend.core.database.healthcheck.DatabaseHealthCheck
import io.github.mudrichenkoevgeny.backend.core.database.healthcheck.RedisHealthCheck
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

/**
 * Dagger module that contributes database-related [HealthCheck] implementations to the app's health set.
 *
 * Binds [DatabaseHealthCheck] and [RedisHealthCheck] into a [Set][kotlin.collections.Set] of [HealthCheck].
 */
@Module
interface DatabaseHealthCheckModule {

    @Binds
    @IntoSet
    fun bindDatabaseHealthCheck(databaseHealthCheck: DatabaseHealthCheck): HealthCheck

    @Binds
    @IntoSet
    fun bindRedisHealthCheck(redisHealthCheck: RedisHealthCheck): HealthCheck

    @Multibinds
    fun bindHealthChecks(): Set<HealthCheck>
}