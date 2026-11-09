package io.github.mudrichenkoevgeny.backend.core.database.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.database.config.factory.DatabaseConfigFactory
import io.github.mudrichenkoevgeny.backend.core.database.config.factory.DatabaseConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import javax.inject.Singleton

/**
 * Dagger module for database configuration.
 *
 * Provides [DatabaseConfigFactory] (bound to [DatabaseConfigFactoryImpl] with [EnvReader])
 * and [DatabaseConfig] from the factory.
 */
@Module
public class DatabaseConfigModule {
    @Provides
    @Singleton
    public fun provideDatabaseConfigFactory(envReader: EnvReader): DatabaseConfigFactory = DatabaseConfigFactoryImpl(
        envReader = envReader
    )

    @Provides
    @Singleton
    public fun provideDatabaseConfig(databaseConfigFactory: DatabaseConfigFactory): DatabaseConfig =
        databaseConfigFactory.create()
}
