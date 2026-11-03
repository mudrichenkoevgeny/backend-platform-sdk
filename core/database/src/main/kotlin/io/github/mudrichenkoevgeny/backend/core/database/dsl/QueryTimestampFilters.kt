package io.github.mudrichenkoevgeny.backend.core.database.dsl

import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

/**
 * Applies timestamp range filtering ([fromInstant] .. [toInstant]) and existence check ([isSet])
 * to an Exposed [Query] for the specified [column].
 *
 * @param column timestamp column expression
 * @param fromInstant optional lower bound timestamp (inclusive)
 * @param toInstant optional upper bound timestamp (inclusive)
 * @param isSet optional flag indicating whether the timestamp must be non-null (`true`) or null (`false`)
 * @return this [Query] with timestamp conditions applied
 */
fun Query.applyTimestampFilter(
    column: ExpressionWithColumnType<out JavaInstant?>,
    fromInstant: KotlinInstant? = null,
    toInstant: KotlinInstant? = null,
    isSet: Boolean? = null
): Query {
    if (fromInstant != null) {
        val javaFrom = fromInstant.toJavaInstant()
        this.andWhere { column.isNotNull() and (column greaterEq javaFrom) }
    }
    if (toInstant != null) {
        val javaTo = toInstant.toJavaInstant()
        this.andWhere { column.isNotNull() and (column lessEq javaTo) }
    }
    if (isSet == true) {
        this.andWhere { column.isNotNull() }
    } else if (isSet == false) {
        this.andWhere { column.isNull() }
    }
    return this
}
