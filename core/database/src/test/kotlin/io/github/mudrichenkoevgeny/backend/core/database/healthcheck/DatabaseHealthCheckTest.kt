package io.github.mudrichenkoevgeny.backend.core.database.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Connection
import javax.sql.DataSource

class DatabaseHealthCheckTest {

    @Test
    fun `check returns Success when connection is valid`() = runBlocking {
        val connection = mockk<Connection> {
            every { isValid(any()) } returns true
            every { close() } just Runs
        }
        val dataSource = mockk<DataSource> {
            every { getConnection() } returns connection
        }

        val healthCheck = DatabaseHealthCheck(dataSource)

        val result = healthCheck.check()

        assertTrue(result is AppSystemResult.Success)
    }

    @Test
    fun `check returns Error when getConnection throws`() = runBlocking {
        val dataSource = mockk<DataSource> {
            every { connection } throws RuntimeException("connection failed")
        }

        val healthCheck = DatabaseHealthCheck(dataSource)

        val result = healthCheck.check()

        assertTrue(result is AppSystemResult.Error)
    }
}
