package io.github.mudrichenkoevgeny.backend.core.database.dsl

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere

/**
 * Applies SQL `LIKE` substring filter conditions against [column] for a list of search values.
 *
 * Blank entries in [rawValues] are ignored. If multiple non-blank values are present,
 * conditions are joined with `OR`.
 *
 * @param column target database string column
 * @param rawValues list of search fragments
 * @return this [Query] with applied `LIKE` conditions
 */
public fun <T : String?> Query.addOrLikeFilter(
    column: Column<T>,
    rawValues: List<String>
): Query {
    val nonBlank = rawValues.filter { it.isNotBlank() }
    if (nonBlank.isEmpty()) {
        return this
    }

    @Suppress("UNCHECKED_CAST") val predicate = nonBlank
        .map { value ->
            (column.lowerCase() like substringSqlLikePattern(value.lowercase())) as Op<Boolean>
        }.reduce { acc, condition -> acc or condition }

    return this.andWhere { predicate }
}
