package io.github.mudrichenkoevgeny.backend.core.database.config.factory

import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig

/**
 * Factory for building [DatabaseConfig] from environment or other configuration source.
 */
interface DatabaseConfigFactory {

    /**
     * Creates a new database configuration instance.
     *
     * @return configuration with JDBC URL/credentials, migration paths, Redis URL and timeout.
     */
    fun create(): DatabaseConfig
}