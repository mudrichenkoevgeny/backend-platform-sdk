package com.mudrichenkoevgeny.backend.core.database.migrator

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.database.di.qualifiers.DatabaseMigratorFlyway
import org.flywaydb.core.Flyway
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
@DatabaseMigratorFlyway
class FlywayDatabaseMigrator @Inject constructor(
    private val appLogger: AppLogger
): DatabaseMigrator {

    override fun migrate(dataSource: DataSource, resources: List<String>) {
        try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(*resources.toTypedArray())
                .baselineOnMigrate(true)
                .load()

            flyway.migrate()
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
        }
    }
}