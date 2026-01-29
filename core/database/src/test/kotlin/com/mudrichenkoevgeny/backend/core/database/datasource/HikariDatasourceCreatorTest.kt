/*
package io.github.mudrichenkoevgeny.backend.core.database.datasource

import io.github.mudrichenkoevgeny.backend.core.database.di.DaggerTestDatabaseComponent
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.sql.Connection

class HikariDatasourceCreatorTest {

    private val component = DaggerTestDatabaseComponent.create()
    private val datasourceCreator = component.dataSourceCreator()

    @Test
    fun `create returns datasource with correct configuration`() {
        val ds = datasourceCreator.create(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            user = "sa",
            password = ""
        ) as HikariDataSource

        assertEquals("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", ds.jdbcUrl)
        assertEquals("sa", ds.username)
        assertEquals("", ds.password)
        assertEquals(component.driverClassName() , ds.driverClassName)
        assertEquals(10, ds.maximumPoolSize)
        assertFalse(ds.isAutoCommit)

        ds.connection.use { conn ->
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, conn.transactionIsolation)
        }

        assertDoesNotThrow { ds.close() }
    }
}
*/
