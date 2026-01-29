package io.github.mudrichenkoevgeny.backend.core.database.migrator

import javax.sql.DataSource

interface DatabaseMigrator {
    /**
     * Applies database schema migrations using provided migration resources.
     *
     * @param dataSource A JDBC-compatible DataSource used to perform migrations.
     * @param resources Classpath migration resource locations (e.g. "db/migration", "migrations/liquibase").
     *                  Exact interpretation depends on the concrete migrator implementation
     *                  (Flyway: directories; Liquibase: changelog file paths).
     */
    fun migrate(dataSource: DataSource, resources: List<String>)
}