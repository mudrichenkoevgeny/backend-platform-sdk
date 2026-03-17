package io.github.mudrichenkoevgeny.backend.core.database.migrator

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DatabaseMigratorFlyway
import org.flywaydb.core.Flyway
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * Default [DatabaseMigrator] implementation using Flyway.
 *
 * Configures Flyway with the given [DataSource] and location list (plus a fixed `classpath:db/migration` for SDK migrations),
 * baselineOnMigrate and outOfOrder enabled, failOnMissingLocations false. On failure logs via [AppLogger] and rethrows.
 */
@Singleton
@DatabaseMigratorFlyway
class FlywayDatabaseMigrator @Inject constructor(
    private val appLogger: AppLogger
): DatabaseMigrator {

    override fun migrate(dataSource: DataSource, resources: List<String>) {
        try {
            val sdkBaseLocation = "classpath:db/migration"
            val allLocations = (resources + sdkBaseLocation).distinct()

            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(*allLocations.toTypedArray())
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .failOnMissingLocations(false)
                .load()

            flyway.migrate()
        } catch (t: Throwable) {
            appLogger.logError(CommonError.Internal(t))
            throw t
        }
    }
}