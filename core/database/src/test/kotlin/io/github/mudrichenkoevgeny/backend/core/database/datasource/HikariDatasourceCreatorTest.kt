package io.github.mudrichenkoevgeny.backend.core.database.datasource

import com.zaxxer.hikari.HikariDataSource
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.sql.Connection

class HikariDatasourceCreatorTest {

    private companion object {
        private const val H2_URL = "jdbc:h2:mem:test_creator;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        private const val USER = "sa"
        private const val PASSWORD = ""
        private const val DRIVER = "org.h2.Driver"
    }

    private val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    private val appLogger = mockk<AppLogger>(relaxUnitFun = true) {
        every { logError(any()) } just runs
    }

    private val creator = HikariDatasourceCreator(
        hikariDriverClassName = DRIVER,
        prometheusMeterRegistry = prometheusMeterRegistry,
        appLogger = appLogger
    )

    @Test
    fun `create returns HikariDataSource with correct url user password and pool config`() {
        val ds = creator.create(url = H2_URL, user = USER, password = PASSWORD) as HikariDataSource

        assertEquals(H2_URL, ds.jdbcUrl)
        assertEquals(USER, ds.username)
        assertEquals(PASSWORD, ds.password)
        assertEquals(DRIVER, ds.driverClassName)
        assertEquals(10, ds.maximumPoolSize)
        assertFalse(ds.isAutoCommit)

        ds.connection.use { conn ->
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, conn.transactionIsolation)
        }

        ds.close()
    }

    @Test
    fun `create logs and rethrows when validation fails`() {
        val invalidUrl = "jdbc:invalid:xxx"
        val appLoggerSpy = mockk<AppLogger>(relaxUnitFun = true)

        val creatorWithSpy = HikariDatasourceCreator(
            hikariDriverClassName = DRIVER,
            prometheusMeterRegistry = prometheusMeterRegistry,
            appLogger = appLoggerSpy
        )

        try {
            creatorWithSpy.create(url = invalidUrl, user = USER, password = PASSWORD)
        } catch (_: Throwable) {
        }

        verify(exactly = 1) { appLoggerSpy.logError(any()) }
    }
}
