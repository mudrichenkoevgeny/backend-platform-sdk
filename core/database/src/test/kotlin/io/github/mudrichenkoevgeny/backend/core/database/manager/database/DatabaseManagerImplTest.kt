package io.github.mudrichenkoevgeny.backend.core.database.manager.database

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.migrator.DatabaseMigrator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

class DatabaseManagerImplTest {

    private val migrationPaths = listOf("classpath:db/migration")

    @Test
    fun `create connects and runs migrator then returns Database`() {
        val dataSource = createTestDataSource("manager_create")
        val databaseMigrator = mockk<DatabaseMigrator> {
            every { migrate(any(), any()) } returns Unit
        }
        val databaseConfig = DatabaseConfig(
            dbUrl = "jdbc:postgresql://localhost/db",
            dbUser = "u",
            dbPassword = "p",
            migrationPaths = migrationPaths,
            redisUrl = "redis://localhost",
            redisTimeoutSeconds = 10L
        )
        val appLogger = mockk<AppLogger>(relaxUnitFun = true)

        val manager = DatabaseManagerImpl(
            dataSource = dataSource,
            databaseMigrator = databaseMigrator,
            databaseConfig = databaseConfig,
            appLogger = appLogger
        )

        val database = manager.create()

        assertNotNull(database)
        verify(exactly = 1) { databaseMigrator.migrate(dataSource, migrationPaths) }
    }

    @Test
    fun `create logs and rethrows when migrator throws`() {
        val dataSource = mockk<DataSource>()
        val databaseMigrator = mockk<DatabaseMigrator> {
            every { migrate(any(), any()) } throws RuntimeException("migrate failed")
        }
        val databaseConfig = DatabaseConfig(
            dbUrl = "jdbc:postgresql://localhost/db",
            dbUser = "u",
            dbPassword = "p",
            migrationPaths = migrationPaths,
            redisUrl = "redis://localhost",
            redisTimeoutSeconds = 10L
        )
        val appLogger = mockk<AppLogger>(relaxUnitFun = true)

        val manager = DatabaseManagerImpl(
            dataSource = dataSource,
            databaseMigrator = databaseMigrator,
            databaseConfig = databaseConfig,
            appLogger = appLogger
        )

        assertThrows<RuntimeException> { manager.create() }
        verify(exactly = 1) { appLogger.logError(any()) }
    }

    @Test
    fun `shutdown closes DataSource when it is AutoCloseable`() {
        val dataSource = TestCloseableDataSource()
        val databaseMigrator = mockk<DatabaseMigrator>()
        val databaseConfig = mockk<DatabaseConfig>()
        val appLogger = mockk<AppLogger>(relaxUnitFun = true)

        val manager = DatabaseManagerImpl(
            dataSource = dataSource,
            databaseMigrator = databaseMigrator,
            databaseConfig = databaseConfig,
            appLogger = appLogger
        )

        manager.shutdown()

        assertTrue(dataSource.closeCalled)
    }

    private class TestCloseableDataSource : DataSource, AutoCloseable {
        var closeCalled = false

        override fun getConnection(): Connection = throw UnsupportedOperationException()
        override fun getConnection(username: String?, password: String?): Connection = throw UnsupportedOperationException()
        override fun getLogWriter(): PrintWriter = throw UnsupportedOperationException()
        override fun setLogWriter(out: PrintWriter?) {}
        override fun getLoginTimeout(): Int = 0
        override fun setLoginTimeout(seconds: Int) {}
        override fun getParentLogger(): Logger = throw UnsupportedOperationException()
        override fun <T : Any> unwrap(iface: Class<T>): T = throw UnsupportedOperationException()
        override fun isWrapperFor(iface: Class<*>): Boolean = false
        override fun close() {
            closeCalled = true
        }
    }
}
