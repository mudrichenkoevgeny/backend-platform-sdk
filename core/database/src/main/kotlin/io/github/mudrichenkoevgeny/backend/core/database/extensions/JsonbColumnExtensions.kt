package io.github.mudrichenkoevgeny.backend.core.database.extensions

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

infix fun Column<Set<String>>.jsonbContainsSingleString(value: String): Op<Boolean> {
    val jsonArray = buildJsonArray { add(JsonPrimitive(value)) }.toString().replace("'", "''")
    return object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            val isH2 = TransactionManager.current().db.vendor.lowercase().contains("h2")
            queryBuilder.append("(")
            if (isH2) {
                queryBuilder.append(this@jsonbContainsSingleString)
                queryBuilder.append(" LIKE '%")
                queryBuilder.append(value)
                queryBuilder.append("%')")
            } else {
                queryBuilder.append(this@jsonbContainsSingleString)
                queryBuilder.append(" @> '")
                queryBuilder.append(jsonArray)
                queryBuilder.append("'::jsonb)")
            }
        }
    }
}

infix fun Column<Set<String>>.jsonbContainsAllStrings(values: Set<String>): Op<Boolean> {
    val jsonArray = buildJsonArray {
        values.forEach { add(JsonPrimitive(it)) }
    }.toString().replace("'", "''")

    return object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            val isH2 = TransactionManager.current().db.vendor.lowercase().contains("h2")
            queryBuilder.append("(")
            if (isH2) {
                if (values.isEmpty()) {
                    queryBuilder.append("1=1")
                } else {
                    values.forEachIndexed { index, value ->
                        queryBuilder.append(this@jsonbContainsAllStrings)
                        queryBuilder.append(" LIKE '%")
                        queryBuilder.append(value)
                        queryBuilder.append("%'")
                        if (index < values.size - 1) queryBuilder.append(" AND ")
                    }
                }
                queryBuilder.append(")")
            } else {
                queryBuilder.append(this@jsonbContainsAllStrings)
                queryBuilder.append(" @> '")
                queryBuilder.append(jsonArray)
                queryBuilder.append("'::jsonb)")
            }
        }
    }
}