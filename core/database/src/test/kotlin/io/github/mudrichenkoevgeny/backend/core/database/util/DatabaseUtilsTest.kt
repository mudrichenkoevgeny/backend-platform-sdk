package io.github.mudrichenkoevgeny.backend.core.database.util

import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.assertThrows

class DbQueryTest {

    private object TestTable : Table("test_query") {
        val id = integer("id").autoIncrement()
        val data = varchar("data", 50)
        override val primaryKey = PrimaryKey(id)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            Database.connect(
                url = "jdbc:h2:mem:db_query_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
        }
    }

    @Test
    fun `dbQuery executes block and returns result`() = runTest {
        dbQuery {
            SchemaUtils.create(TestTable)
        }

        val inputData = "test_value"

        dbQuery {
            TestTable.insert {
                it[data] = inputData
            }
        }

        val result = dbQuery {
            TestTable.selectAll()
                .where { TestTable.data eq inputData }
                .map { it[TestTable.data] }
                .single()
        }

        assertEquals(inputData, result)
    }

    @Test
    fun `dbQuery rethrows exception from block`() = runTest {
        val errorMessage = "Database failure"

        val exception = assertThrows<RuntimeException> {
            dbQuery {
                throw RuntimeException(errorMessage)
            }
        }

        assertEquals(errorMessage, exception.message)
    }
}