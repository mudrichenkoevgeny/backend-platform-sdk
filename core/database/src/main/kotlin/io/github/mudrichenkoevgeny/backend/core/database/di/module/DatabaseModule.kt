package io.github.mudrichenkoevgeny.backend.core.database.di.module

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.datasource.DataSourceCreator
import io.github.mudrichenkoevgeny.backend.core.database.datasource.HikariDatasourceCreator
import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DatabaseMigratorFlyway
import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DriverClassName
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManagerImpl
import io.github.mudrichenkoevgeny.backend.core.database.migrator.DatabaseMigrator
import io.github.mudrichenkoevgeny.backend.core.database.migrator.FlywayDatabaseMigrator
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import org.jetbrains.exposed.sql.Database
import javax.inject.Singleton
import javax.sql.DataSource

@Module
interface DatabaseModule {

    @Binds
    @Singleton
    @DatabaseMigratorFlyway
    fun bindDatabaseMigrator(flywayDatabaseMigrator: FlywayDatabaseMigrator): DatabaseMigrator

    companion object {
        @Provides
        @Singleton
        @DriverClassName
        fun provideDataSourceDriverClassName(): String = "org.postgresql.Driver"

        @Provides
        @Singleton
        fun provideDataSourceCreator(
            @DriverClassName driverClassName: String,
            telemetryProvider: TelemetryProvider,
            appLogger: AppLogger
        ): DataSourceCreator = HikariDatasourceCreator(
            hikariDriverClassName = driverClassName,
            prometheusMeterRegistry = telemetryProvider.prometheusMeterRegistry,
            appLogger = appLogger
        )

        @Provides
        @Singleton
        fun provideDataSource(
            dataSourceCreator: DataSourceCreator,
            databaseConfig: DatabaseConfig
        ): DataSource {
            return dataSourceCreator.create(
                url = databaseConfig.dbUrl,
                user = databaseConfig.dbUser,
                password = databaseConfig.dbPassword
            )
        }

        @Provides
        @Singleton
        fun provideDatabaseManager(
            dataSource: DataSource,
            @DatabaseMigratorFlyway databaseMigrator: DatabaseMigrator,
            databaseTables: Set<@JvmSuppressWildcards BaseTable>,
            databaseConfig: DatabaseConfig,
            appLogger: AppLogger
        ): DatabaseManager {
            return DatabaseManagerImpl(
                dataSource = dataSource,
                databaseMigrator = databaseMigrator,
                databaseTables = databaseTables,
                databaseConfig = databaseConfig,
                appLogger = appLogger
            )
        }

        @Provides
        @Singleton
        fun provideDatabase(factory: DatabaseManager): Database {
            return factory.create()
        }
    }
}