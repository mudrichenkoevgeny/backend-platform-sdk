package io.github.mudrichenkoevgeny.backend.sample.integration

import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import io.github.mudrichenkoevgeny.backend.core.settings.database.table.SystemSettingsTable
import io.github.mudrichenkoevgeny.backend.core.storage.database.table.StorageFilesTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserKnownDevicesTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserTotpSettingsTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer

@RequiresDocker
internal class FlywayValidationIntegrationTest {
    @Test
    fun `flyway migrate should execute cleanly and match exposed tables on a fresh database`() {
        PostgreSQLContainer(POSTGRES_IMAGE).use { postgres ->
            postgres.withDatabaseName(DB_NAME)
            postgres.withUsername(DB_USER)
            postgres.withPassword(DB_PASSWORD)
            postgres.start()

            val flyway = Flyway
                .configure()
                .dataSource(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password
                ).locations(
                    MIGRATION_LOCATION_CORE,
                    MIGRATION_LOCATION_FEATURE
                ).load()

            val migrateResult = flyway.migrate()
            assertTrue(migrateResult.migrationsExecuted > 0)
            assertTrue(migrateResult.success)

            flyway.validate()

            val database = Database.connect(
                url = postgres.jdbcUrl,
                driver = POSTGRES_DRIVER,
                user = postgres.username,
                password = postgres.password
            )

            transaction(database) {
                val statements = SchemaUtils.checkMappingConsistence(
                    AuditEventsTable,
                    SystemSettingsTable,
                    StorageFilesTable,
                    UsersTable,
                    UserIdentifiersTable,
                    UserSessionsTable,
                    UserTotpSettingsTable,
                    UserKnownDevicesTable
                )
                assertTrue(statements.isEmpty()) { "Exposed mapping mismatch: $statements" }
            }
        }
    }

    private companion object {
        private const val POSTGRES_IMAGE = "postgres:16-alpine"
        private const val DB_NAME = "test_db"
        private const val DB_USER = "test_user"
        private const val DB_PASSWORD = "test_password"
        private const val POSTGRES_DRIVER = "org.postgresql.Driver"
        private const val MIGRATION_LOCATION_CORE = "classpath:db/migration/core"
        private const val MIGRATION_LOCATION_FEATURE = "classpath:db/migration/feature"
    }
}
