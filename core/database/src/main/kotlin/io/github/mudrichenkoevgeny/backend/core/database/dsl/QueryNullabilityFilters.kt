package io.github.mudrichenkoevgeny.backend.core.database.dsl

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere

/**
 * Applies a nullability filter (`IS NOT NULL` / `IS NULL`) to an Exposed [Query] for [column].
 *
 * @param column target column or expression
 * @param isSet `true` to require non-null values (`isNotNull()`), `false` to require null values (`isNull()`), or `null` to ignore
 * @return this [Query] with nullability condition applied
 */
fun <T> Query.applyNullabilityFilter(
    column: Expression<T?>,
    isSet: Boolean?
): Query {
    return when (isSet) {
        true -> andWhere { column.isNotNull() }
        false -> andWhere { column.isNull() }
        null -> this
    }
}
