package io.github.mudrichenkoevgeny.backend.core.database.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.database.config.envkeys.DatabaseEnvKeys
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DatabaseConfigFactoryImplTest {

    private companion object {
        private const val DB_URL = "jdbc:postgresql://localhost:5432/test"
        private const val DB_USER = "user"
        private const val DB_PASSWORD = "secret"
        private const val MIGRATION_PATHS = "classpath:db/migration,classpath:app/migration"
        private const val REDIS_URL = "redis://localhost:6379"
        private const val REDIS_TIMEOUT = "5"
        private const val DB_USER_FILE = "db_user.txt"
        private const val DB_PASSWORD_FILE = "db_password.txt"
        private const val REDIS_URL_FILE = "redis_url.txt"
    }

    private val envReader = mockk<EnvReader>()

    @Test
    fun `create returns DatabaseConfig from env and secret files`() {
        every { envReader.getByKey(DatabaseEnvKeys.DB_USER_SECRET_FILE) } returns DB_USER_FILE
        every { envReader.getByKey(DatabaseEnvKeys.DB_PASSWORD_SECRET_FILE) } returns DB_PASSWORD_FILE
        every { envReader.getByKey(DatabaseEnvKeys.REDIS_URL_SECRET_FILE) } returns REDIS_URL_FILE
        every { envReader.getByKey(DatabaseEnvKeys.DB_URL) } returns DB_URL
        every { envReader.readSecret(DB_USER_FILE) } returns DB_USER
        every { envReader.readSecret(DB_PASSWORD_FILE) } returns DB_PASSWORD
        every { envReader.getByKeyOrNull(DatabaseEnvKeys.MIGRATION_PATHS) } returns MIGRATION_PATHS
        every { envReader.readSecret(REDIS_URL_FILE) } returns REDIS_URL
        every { envReader.getByKey(DatabaseEnvKeys.REDIS_TIMEOUT_SECONDS) } returns REDIS_TIMEOUT

        val factory = DatabaseConfigFactoryImpl(envReader)

        val config: DatabaseConfig = factory.create()

        assertEquals(DB_URL, config.dbUrl)
        assertEquals(DB_USER, config.dbUser)
        assertEquals(DB_PASSWORD, config.dbPassword)
        assertEquals(listOf("classpath:db/migration", "classpath:app/migration"), config.migrationPaths)
        assertEquals(REDIS_URL, config.redisUrl)
        assertEquals(5L, config.redisTimeoutSeconds)
    }

    @Test
    fun `create uses defaultMigrationPaths when MIGRATION_PATHS is null`() {
        every { envReader.getByKey(DatabaseEnvKeys.DB_USER_SECRET_FILE) } returns DB_USER_FILE
        every { envReader.getByKey(DatabaseEnvKeys.DB_PASSWORD_SECRET_FILE) } returns DB_PASSWORD_FILE
        every { envReader.getByKey(DatabaseEnvKeys.REDIS_URL_SECRET_FILE) } returns REDIS_URL_FILE
        every { envReader.getByKey(DatabaseEnvKeys.DB_URL) } returns DB_URL
        every { envReader.readSecret(DB_USER_FILE) } returns DB_USER
        every { envReader.readSecret(DB_PASSWORD_FILE) } returns DB_PASSWORD
        every { envReader.getByKeyOrNull(DatabaseEnvKeys.MIGRATION_PATHS) } returns null
        every { envReader.readSecret(REDIS_URL_FILE) } returns REDIS_URL
        every { envReader.getByKey(DatabaseEnvKeys.REDIS_TIMEOUT_SECONDS) } returns REDIS_TIMEOUT

        val factory = DatabaseConfigFactoryImpl(envReader)

        val config: DatabaseConfig = factory.create()

        assertEquals(DatabaseConfig.defaultMigrationPaths, config.migrationPaths)
    }
}
