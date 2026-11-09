package io.github.mudrichenkoevgeny.backend.core.database.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.database.config.factory.DatabaseConfigFactory
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig

/**
 * Environment variable names used to build [DatabaseConfig].
 * Consumed by [DatabaseConfigFactory] implementations.
 */
public object DatabaseEnvKeys {
    public const val DB_URL: String = "DB_URL"
    public const val DB_USER_SECRET_FILE: String = "DB_USER_SECRET_FILE"
    public const val DB_PASSWORD_SECRET_FILE: String = "DB_PASSWORD_SECRET_FILE"
    public const val MIGRATION_PATHS: String = "MIGRATION_PATHS"
    public const val DB_MIGRATE_ON_STARTUP: String = "DB_MIGRATE_ON_STARTUP"
    public const val REDIS_URL_SECRET_FILE: String = "REDIS_URL_SECRET_FILE"
    public const val REDIS_TIMEOUT_SECONDS: String = "REDIS_TIMEOUT_SECONDS"
}
