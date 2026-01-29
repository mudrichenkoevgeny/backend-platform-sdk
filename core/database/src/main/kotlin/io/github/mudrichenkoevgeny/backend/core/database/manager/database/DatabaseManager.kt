package io.github.mudrichenkoevgeny.backend.core.database.manager.database

import org.jetbrains.exposed.sql.Database

interface DatabaseManager {
    /**
     * Creates and initializes a database instance.
     *
     * Establishes a connection using the provided JDBC configuration, applies schema
     * migrations, and ensures required tables exist.
     *
     * @param url JDBC URL of the database (e.g. "jdbc:postgresql://localhost:5432/mydb")
     * @param user Username for database authentication
     * @param password Password for database authentication
     * @param tables One or more sets of Exposed tables that must be created if missing.
     *               Each argument is treated as a logical group of tables.
     * @param migrationResources Classpath migration resource locations (e.g. "db/migration", "migrations/liquibase").
     *                  Exact interpretation depends on the concrete migrator implementation
     *                  (Flyway: directories; Liquibase: changelog file paths).
     */
    fun create(): Database

    fun shutdown()
}