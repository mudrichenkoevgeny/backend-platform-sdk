package io.github.mudrichenkoevgeny.backend.core.database.integration

import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer

@RequiresDocker
internal class FlywayValidationIntegrationTest {
    @Test
    fun `flyway migrate should execute cleanly on a fresh database`() {
        PostgreSQLContainer("postgres:16-alpine").use { postgres ->
            postgres.withDatabaseName("test_db")
            postgres.withUsername("test_user")
            postgres.withPassword("test_password")
            postgres.start()

            val flyway = Flyway
                .configure()
                .dataSource(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password
                ).locations(
                    "classpath:db/migration/core",
                    "classpath:db/migration/feature"
                ).load()

            val migrateResult = flyway.migrate()
            assertTrue(migrateResult.success)
        }
    }
}
