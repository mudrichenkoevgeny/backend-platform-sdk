package io.github.mudrichenkoevgeny.backend.core.database.dsl

import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.database.table.TestTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Instant as KotlinInstant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryTimestampFiltersTest {

    private val dataSource = createTestDataSource("timestamp_filter")

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.create(TestTable)
            }
        }
    }

    @Test
    fun `applyTimestampFilter applies range and nullability filters to Query`() = runBlocking {
        suspendTransaction {
            val fromInstant = KotlinInstant.parse("2026-01-01T00:00:00Z")
            val toInstant = KotlinInstant.parse("2026-12-31T23:59:59Z")

            val query = TestTable.selectAll().applyTimestampFilter(
                column = TestTable.createdAt,
                fromInstant = fromInstant,
                toInstant = toInstant,
                isSet = true
            )

            assertNotNull(query)
        }
    }

    @Test
    fun `applyTimestampFilter applies isSet false filter to Query`() = runBlocking {
        suspendTransaction {
            val query = TestTable.selectAll().applyTimestampFilter(
                column = TestTable.updatedAt,
                isSet = false
            )

            assertNotNull(query)
        }
    }
}
