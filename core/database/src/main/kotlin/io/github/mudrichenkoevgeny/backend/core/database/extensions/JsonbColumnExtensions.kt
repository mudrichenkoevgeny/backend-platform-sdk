package io.github.mudrichenkoevgeny.backend.core.database.extensions

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder

/**
 * PostgreSQL JSONB containment on the receiver: SQL `column @> '["…"]'::jsonb` where the right-hand side is a
 * single-element JSON array built from [value].
 *
 * Not `private`: must stay visible to other Gradle modules (e.g. `feature:user`) that build queries; file-level
 * `private` would limit visibility to this file only.
 */
infix fun Column<Set<String>>.jsonbContainsSingleString(value: String): Op<Boolean> {
    val jsonArray = buildJsonArray { add(JsonPrimitive(value)) }.toString().replace("'", "''")
    return object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            queryBuilder.append("(")
            queryBuilder.append(this@jsonbContainsSingleString)
            queryBuilder.append(" @> '")
            queryBuilder.append(jsonArray)
            queryBuilder.append("'::jsonb)")
        }
    }
}
