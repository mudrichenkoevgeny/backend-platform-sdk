package io.github.mudrichenkoevgeny.backend.core.database.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.datasource.DataSourceCreator
import io.github.mudrichenkoevgeny.backend.core.database.datasource.HikariDatasourceCreator
import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DriverClassName
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManagerImpl
import io.github.mudrichenkoevgeny.backend.core.database.migrator.DatabaseMigrator
import io.github.mudrichenkoevgeny.backend.core.database.migrator.FlywayDatabaseMigrator
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import org.jetbrains.exposed.v1.jdbc.Database
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * Dagger module for PostgreSQL DataSource, Exposed Database, and [DatabaseManager].
 *
 * Binds [DatabaseMigrator] to [FlywayDatabaseMigrator].
 * Provides [DriverClassName] (PostgreSQL driver), [DataSourceCreator] (→ [HikariDatasourceCreator]),
 * [DataSource] from config, [DatabaseManager] (→ [DatabaseManagerImpl]), and Exposed [Database] from the manager.
 */
@Module
public interface DatabaseModule {
    @Binds
    @Singleton
    public fun bindDatabaseMigrator(flywayDatabaseMigrator: FlywayDatabaseMigrator): DatabaseMigrator

    public companion object {
        @Provides
        @Singleton
        @DriverClassName
        public fun provideDataSourceDriverClassName(): String = "org.postgresql.Driver"

        @Provides
        @Singleton
        public fun provideDataSourceCreator(
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
        public fun provideDataSource(
            dataSourceCreator: DataSourceCreator,
            databaseConfig: DatabaseConfig
        ): DataSource = dataSourceCreator.create(
            url = databaseConfig.dbUrl,
            user = databaseConfig.dbUser,
            password = databaseConfig.dbPassword
        )

        @Provides
        @Singleton
        public fun provideDatabaseManager(
            dataSource: DataSource,
            databaseMigrator: DatabaseMigrator,
            databaseConfig: DatabaseConfig,
            appLogger: AppLogger
        ): DatabaseManager = DatabaseManagerImpl(
            dataSource = dataSource,
            databaseMigrator = databaseMigrator,
            databaseConfig = databaseConfig,
            appLogger = appLogger
        )

        @Provides
        @Singleton
        public fun provideDatabase(factory: DatabaseManager): Database = factory.create()
    }
}
