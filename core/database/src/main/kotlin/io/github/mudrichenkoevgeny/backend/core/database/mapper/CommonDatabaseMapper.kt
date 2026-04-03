package io.github.mudrichenkoevgeny.backend.core.database.mapper

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import org.jetbrains.exposed.v1.core.SortOrder as ExposedSortOrder

/**
 * Maps the shared foundation listing [SortOrder] to Exposed order direction for `Query.orderBy`.
 */
fun SortOrder.toExposedSortOrder(): ExposedSortOrder = when (this) {
    SortOrder.ASC -> ExposedSortOrder.ASC
    SortOrder.DESC -> ExposedSortOrder.DESC
}
