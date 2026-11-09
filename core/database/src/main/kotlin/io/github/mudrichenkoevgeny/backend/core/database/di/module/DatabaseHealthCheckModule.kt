package io.github.mudrichenkoevgeny.backend.core.database.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import io.github.mudrichenkoevgeny.backend.core.database.healthcheck.DatabaseHealthCheck
import io.github.mudrichenkoevgeny.backend.core.database.healthcheck.RedisHealthCheck

/**
 * Dagger module that contributes database-related [HealthCheck] implementations to the app's health set.
 *
 * Binds [DatabaseHealthCheck] and [RedisHealthCheck] into a [Set] of [HealthCheck].
 */
@Module
public interface DatabaseHealthCheckModule {
    @Binds
    @IntoSet
    public fun bindDatabaseHealthCheck(databaseHealthCheck: DatabaseHealthCheck): HealthCheck

    @Binds
    @IntoSet
    public fun bindRedisHealthCheck(redisHealthCheck: RedisHealthCheck): HealthCheck

    @Multibinds
    public fun bindHealthChecks(): Set<HealthCheck>
}
