package io.github.mudrichenkoevgeny.backend.core.database.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.database.config.factory.DatabaseConfigFactory
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig

/**
 * Environment variable names used to build [DatabaseConfig].
 * Consumed by [DatabaseConfigFactory] implementations.
 */
object DatabaseEnvKeys {
    const val DB_URL = "DB_URL"
    const val DB_USER_SECRET_FILE = "DB_USER_SECRET_FILE"
    const val DB_PASSWORD_SECRET_FILE = "DB_PASSWORD_SECRET_FILE"
    const val MIGRATION_PATHS = "MIGRATION_PATHS"
    const val REDIS_URL_SECRET_FILE = "REDIS_URL_SECRET_FILE"
    const val REDIS_TIMEOUT_SECONDS = "REDIS_TIMEOUT_SECONDS"
}