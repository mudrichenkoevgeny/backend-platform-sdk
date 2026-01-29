/*
package io.github.mudrichenkoevgeny.backend.core.database.factory

import io.github.mudrichenkoevgeny.backend.core.database.datasource.DataSourceCreator
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManagerImpl
import io.github.mudrichenkoevgeny.backend.core.database.migrator.DatabaseMigrator
import io.github.mudrichenkoevgeny.backend.core.database.table.TestTable
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.h2.jdbcx.JdbcDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.sql.DataSource

class DatabaseFactoryImplTest {

    private val dataSourceUrl = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
    private val dataSourceUser = "sa"
    private val dataSourcePassword = ""

    private lateinit var dataSource: JdbcDataSource

    @BeforeEach
    fun init() {
        dataSource = JdbcDataSource().apply {
            setURL(dataSourceUrl)
            user = dataSourceUser
            password = dataSourcePassword
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(Database.connect(dataSource)) {
            SchemaUtils.drop(TestTable)
        }
    }

    @Test
    fun `create - creates tables and runs migrations`() = runBlocking {
        // GIVEN
        val dataSourceCreator = createDataSourceCreator(dataSource)
        val databaseMigrator = createDatabaseMigrator()
        val factory = DatabaseManagerImpl(dataSourceCreator, databaseMigrator)

        // WHEN
        factory.create(
            url = dataSource.getURL(),
            user = dataSource.user,
            password = dataSource.password,
            tables = setOf(TestTable),
            migrationResources = listOf(),
            migrationPlaceholders = mapOf()
        )

        // THEN
        verify { dataSourceCreator.create(any(), any(), any()) }
        verify { databaseMigrator.migrate(dataSource, any(), any()) }

        transaction {
            assertTrue(TestTable.exists())
        }
    }

    @Test
    fun `create - does not create tables if empty set`() = runBlocking {
        // GIVEN
        val dataSourceCreator = createDataSourceCreator(dataSource)
        val databaseMigrator = createDatabaseMigrator()
        val factory = DatabaseManagerImpl(dataSourceCreator, databaseMigrator)

        // WHEN
        factory.create(
            url = dataSource.getURL(),
            user = dataSource.user,
            password = dataSource.password,
            tables = emptySet(),
            migrationResources = listOf(),
            migrationPlaceholders = mapOf()
        )

        // THEN
        transaction {
            assertTrue(!TestTable.exists())
        }

        verify { databaseMigrator.migrate(dataSource, any(), any()) }
    }

    @Test
    fun `create - propagates exception from dataSourceCreator`() {
        // GIVEN
        val dataSourceCreator = createDataSourceCreator(dataSource, true)
        val databaseMigrator = createDatabaseMigrator()
        val factory = DatabaseManagerImpl(dataSourceCreator, databaseMigrator)

        // WHEN + THEN
        assertThrows<RuntimeException> {
            factory.create(
                url = dataSource.getURL(),
                user = dataSource.user,
                password = dataSource.password,
                tables = setOf(TestTable),
                migrationResources = listOf(),
                migrationPlaceholders = mapOf()
            )
        }
    }

    @Test
    fun `create - propagates exception from databaseMigrator`() {
        // GIVEN
        val dataSourceCreator = createDataSourceCreator(dataSource)
        val databaseMigrator = createDatabaseMigrator(true)
        val factory = DatabaseManagerImpl(dataSourceCreator, databaseMigrator)

        // WHEN + THEN
        assertThrows<RuntimeException> {
            factory.create(
                url = dataSource.getURL(),
                user = dataSource.user,
                password = dataSource.password,
                tables = setOf(TestTable),
                migrationResources = listOf("db/changelog"),
                migrationPlaceholders = mapOf()
            )
        }
    }

    private fun createDataSourceCreator(dataSource: DataSource, throwsError: Boolean = false): DataSourceCreator {
        return if (throwsError) {
            mockk<DataSourceCreator> {
                every { create(any(), any(), any()) } throws
                        RuntimeException("DataSourceCreator error")
            }
        } else {
            mockk<DataSourceCreator> {
                every { create(any(), any(), any()) } returns dataSource
            }
        }
    }

    private fun createDatabaseMigrator(throwsError: Boolean = false): DatabaseMigrator {
        return if (throwsError) {
            mockk<DatabaseMigrator> {
                every { migrate(dataSource, any(), any()) } throws
                        RuntimeException("DatabaseMigrator error")
            }
        } else {
            mockk<DatabaseMigrator> {
                every { migrate(dataSource, any(), any()) } just Runs
            }
        }
    }
}
*/
