package io.github.mudrichenkoevgeny.backend.core.database.extensions

import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.jsonb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JsonbColumnExtensionsTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            Database.connect("jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", driver = "org.h2.Driver")
        }
    }

    object TestTable : Table("test_table") {
        val tags = jsonb<Set<String>>(
            "tags",
            FoundationJson,
            serializer<Set<String>>()
        )
    }

    @Test
    fun `jsonbContainsSingleString generates correct SQL`() = transaction {
        val value = "admin"
        val op = TestTable.tags jsonbContainsSingleString value

        val queryBuilder = QueryBuilder(false)
        op.toQueryBuilder(queryBuilder)

        assertEquals("(test_table.tags like '%admin%')", queryBuilder.toString().lowercase())
    }

    @Test
    fun `jsonbContainsSingleString escapes single quotes in value`() = transaction {
        val value = "user's"
        val op = TestTable.tags jsonbContainsSingleString value

        val queryBuilder = QueryBuilder(false)
        op.toQueryBuilder(queryBuilder)

        assertEquals("(test_table.tags like '%user's%')", queryBuilder.toString().lowercase())
    }

    @Test
    fun `jsonbContainsAllStrings generates correct SQL for multiple values`() = transaction {
        val values = setOf("kotlin", "exposed")
        val op = TestTable.tags jsonbContainsAllStrings values

        val queryBuilder = QueryBuilder(false)
        op.toQueryBuilder(queryBuilder)

        assertEquals("(test_table.tags like '%kotlin%' and test_table.tags like '%exposed%')", queryBuilder.toString().lowercase())
    }

    @Test
    fun `jsonbContainsAllStrings handles empty set`() = transaction {
        val op = TestTable.tags jsonbContainsAllStrings emptySet()

        val queryBuilder = QueryBuilder(false)
        op.toQueryBuilder(queryBuilder)

        assertEquals("(1=1)", queryBuilder.toString().lowercase())
    }
}