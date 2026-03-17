package io.github.mudrichenkoevgeny.backend.core.database.table

import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import kotlin.uuid.Uuid

/**
 * Base Exposed table with UUID primary key and audit timestamps.
 *
 * Subclass for SDK or app tables that need a standard `id` ([Uuid]), `created_at` (default current timestamp),
 * and optional `updated_at`. Extends [IdTable].
 *
 * @param name table name in the database.
 */
open class BaseTable(name: String) : IdTable<Uuid>(name) {
    override val id = uuid("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()
}