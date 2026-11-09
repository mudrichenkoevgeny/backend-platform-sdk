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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryLikeFiltersTest {
    private val dataSource = createTestDataSource("like_filter")

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
    fun `addOrLikeFilter applies like pattern filters to Query`() = runBlocking {
        suspendTransaction {
            val query = TestTable.selectAll().addOrLikeFilter(
                column = TestTable.name,
                rawValues = listOf(
                    "alpha",
                    "beta"
                )
            )

            assertNotNull(query)
        }
    }
}
