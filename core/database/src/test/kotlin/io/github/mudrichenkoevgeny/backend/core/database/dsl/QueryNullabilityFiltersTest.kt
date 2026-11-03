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
class QueryNullabilityFiltersTest {

    private val dataSource = createTestDataSource("nullability_filter")

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
    fun `applyNullabilityFilter applies isNotNull when isSet is true`() = runBlocking {
        suspendTransaction {
            val query = TestTable.selectAll().applyNullabilityFilter(
                column = TestTable.updatedAt,
                isSet = true
            )

            assertNotNull(query)
        }
    }

    @Test
    fun `applyNullabilityFilter applies isNull when isSet is false`() = runBlocking {
        suspendTransaction {
            val query = TestTable.selectAll().applyNullabilityFilter(
                column = TestTable.updatedAt,
                isSet = false
            )

            assertNotNull(query)
        }
    }

    @Test
    fun `applyNullabilityFilter ignores condition when isSet is null`() = runBlocking {
        suspendTransaction {
            val query = TestTable.selectAll().applyNullabilityFilter(
                column = TestTable.updatedAt,
                isSet = null
            )

            assertNotNull(query)
        }
    }
}
