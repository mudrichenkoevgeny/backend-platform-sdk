package io.github.mudrichenkoevgeny.backend.core.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Base Exposed table with UUID primary key and audit timestamps.
 *
 * Subclass for SDK or app tables that need a standard `id` ([Uuid]), `created_at` (default current timestamp),
 * and optional `updated_at`. Extends [IdTable].
 *
 * @param name table name in the database.
 */
public open class BaseTable(
    name: String
) : IdTable<Uuid>(name) {
    override val id: Column<EntityID<Uuid>> = uuid("id").entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)

    public val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(CurrentTimestamp)
    public val updatedAt: Column<Instant?> = timestamp("updated_at").nullable()
}
