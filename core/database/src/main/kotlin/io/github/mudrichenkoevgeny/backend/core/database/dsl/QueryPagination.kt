package io.github.mudrichenkoevgeny.backend.core.database.dsl

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import org.jetbrains.exposed.v1.jdbc.Query

/**
 * Applies [PageParams] limit and offset to this Exposed [Query].
 *
 * @param params pagination parameters (limit, offset).
 * @return this [Query] with [Query.limit] and [Query.offset] applied.
 */
public fun Query.applyPagination(params: PageParams): Query = this.limit(params.limit).offset(params.offset)
