package io.github.mudrichenkoevgeny.backend.core.database.di.module

import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import io.github.mudrichenkoevgeny.backend.core.database.healthcheck.DatabaseHealthCheck
import io.github.mudrichenkoevgeny.backend.core.database.healthcheck.RedisHealthCheck
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
interface DatabaseHealthCheckModule {

    @Binds
    @IntoSet
    fun bindDatabaseHealthCheck(databaseHealthCheck: DatabaseHealthCheck): HealthCheck

    @Binds
    @IntoSet
    fun bindRedisHealthCheck(redisHealthCheck: RedisHealthCheck): HealthCheck
}