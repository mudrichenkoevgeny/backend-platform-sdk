package io.github.mudrichenkoevgeny.backend.core.database.manager.database

import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

/**
 * Creates and manages the Exposed [Database] instance: connect via the configured [DataSource],
 * run migrations, and optionally shut down the underlying pool.
 */
interface DatabaseManager {

    /**
     * Creates and initializes the database instance: connects using the injected DataSource,
     * runs schema migrations via the configured DatabaseMigrator, and returns the Exposed [Database].
     *
     * @return Exposed [Database] connected to the configured PostgreSQL instance.
     */
    fun create(): Database

    /**
     * Shuts down the underlying DataSource (e.g. closes the HikariCP pool).
     */
    fun shutdown()
}