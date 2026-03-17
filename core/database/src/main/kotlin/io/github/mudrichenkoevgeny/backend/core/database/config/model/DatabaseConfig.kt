package io.github.mudrichenkoevgeny.backend.core.database.config.model

/**
 * Configuration for PostgreSQL (JDBC) and Redis used by the database module.
 *
 * @param dbUrl JDBC URL (e.g. `jdbc:postgresql://host:5432/db`).
 * @param dbUser Database username.
 * @param dbPassword Database password.
 * @param migrationPaths Classpath locations for Flyway migrations (e.g. `classpath:db/migration`). Defaults to [defaultMigrationPaths] in companion.
 * @param redisUrl Redis connection URL.
 * @param redisTimeoutSeconds Redis connection timeout in seconds.
 */
data class DatabaseConfig(
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val migrationPaths: List<String> = defaultMigrationPaths,
    val redisUrl: String,
    val redisTimeoutSeconds: Long
) {

    companion object {
        /** Default Flyway locations when migrationPaths is not overridden. */
        val defaultMigrationPaths = listOf("classpath:db/migration")
    }
}