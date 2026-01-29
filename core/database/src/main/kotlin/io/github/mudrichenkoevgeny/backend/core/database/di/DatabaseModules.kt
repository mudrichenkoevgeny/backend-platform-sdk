package io.github.mudrichenkoevgeny.backend.core.database.di

import io.github.mudrichenkoevgeny.backend.core.database.di.module.DatabaseConfigModule
import io.github.mudrichenkoevgeny.backend.core.database.di.module.DatabaseHealthCheckModule
import io.github.mudrichenkoevgeny.backend.core.database.di.module.DatabaseModule
import io.github.mudrichenkoevgeny.backend.core.database.di.module.RedisModule
import dagger.Module

@Module(
    includes = [
        DatabaseConfigModule::class,
        DatabaseModule::class,
        RedisModule::class,
        DatabaseHealthCheckModule::class
    ]
)
interface DatabaseModules