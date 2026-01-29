package io.github.mudrichenkoevgeny.backend.core.database.extensions

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import org.jetbrains.exposed.sql.Query

fun Query.applyPagination(params: PageParams): Query {
    return this.limit(params.limit).offset(params.offset)
}