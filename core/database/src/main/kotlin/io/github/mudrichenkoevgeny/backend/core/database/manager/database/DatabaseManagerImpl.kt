package io.github.mudrichenkoevgeny.backend.core.database.manager.database

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.migrator.DatabaseMigrator
import org.jetbrains.exposed.v1.jdbc.Database
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
class DatabaseManagerImpl @Inject constructor(
    private val dataSource: DataSource,
    private val databaseMigrator: DatabaseMigrator,
    private val databaseConfig: DatabaseConfig,
    private val appLogger: AppLogger
): DatabaseManager {

    override fun create(): Database {
        return try {
            val database = Database.connect(dataSource)

            databaseMigrator.migrate(dataSource, databaseConfig.migrationPaths)

            database
        } catch (t: Throwable) {
            appLogger.logError(CommonError.Internal(t))
            throw t
        }
    }

    override fun shutdown() {
        (dataSource as? AutoCloseable)?.close()
    }
}