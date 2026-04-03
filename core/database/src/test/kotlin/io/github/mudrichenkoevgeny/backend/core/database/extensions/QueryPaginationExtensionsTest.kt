package io.github.mudrichenkoevgeny.backend.core.database.extensions

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
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
class QueryPaginationExtensionsTest {

    private val dataSource = createTestDataSource("pagination")

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
    fun `applyPagination applies limit and offset to Query without throwing`() = runBlocking {
        suspendTransaction {
            val query = TestTable.selectAll().applyPagination(PageParams(page = 1, size = 10))

            assertNotNull(query)
        }
    }
}
