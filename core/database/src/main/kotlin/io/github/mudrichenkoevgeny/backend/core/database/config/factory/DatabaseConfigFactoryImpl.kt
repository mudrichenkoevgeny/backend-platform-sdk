package io.github.mudrichenkoevgeny.backend.core.database.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.database.config.envkeys.DatabaseEnvKeys
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): DatabaseConfigFactory {

    override fun create(): DatabaseConfig {
        // secret files
        val dbUserFile = envReader.getByKey(DatabaseEnvKeys.DB_USER_SECRET_FILE)
        val dbPasswordFile = envReader.getByKey(DatabaseEnvKeys.DB_PASSWORD_SECRET_FILE)
        val redisUrlFile = envReader.getByKey(DatabaseEnvKeys.REDIS_URL_SECRET_FILE)

        // env
        val dbUrl = envReader.getByKey(DatabaseEnvKeys.DB_URL)
        val dbUser = envReader.readSecret(dbUserFile)
        val dbPassword = envReader.readSecret(dbPasswordFile)

        val migrationPaths = envReader.getByKeyOrNull(DatabaseEnvKeys.MIGRATION_PATHS)
            ?.split(",")
            ?: DatabaseConfig.defaultMigrationPaths

        val redisUrl = envReader.readSecret(redisUrlFile)
        val redisTimeoutSeconds = envReader.getByKey(DatabaseEnvKeys.REDIS_TIMEOUT_SECONDS).toLong()

        return DatabaseConfig(
            dbUrl = dbUrl,
            dbUser = dbUser,
            dbPassword = dbPassword,
            migrationPaths = migrationPaths,
            redisUrl = redisUrl,
            redisTimeoutSeconds = redisTimeoutSeconds
        )
    }
}