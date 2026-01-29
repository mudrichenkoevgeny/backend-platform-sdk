package io.github.mudrichenkoevgeny.backend.core.database.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.database.config.factory.DatabaseConfigFactory
import io.github.mudrichenkoevgeny.backend.core.database.config.factory.DatabaseConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseConfigModule {

    @Provides
    @Singleton
    fun provideDatabaseConfigFactory(
        envReader: EnvReader
    ): DatabaseConfigFactory {
        return DatabaseConfigFactoryImpl(
            envReader = envReader
        )
    }

    @Provides
    @Singleton
    fun provideDatabaseConfig(
        databaseConfigFactory: DatabaseConfigFactory
    ): DatabaseConfig {
        return databaseConfigFactory.create()
    }
}