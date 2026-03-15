package io.github.mudrichenkoevgeny.backend.core.database.table

import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import kotlin.uuid.Uuid

open class BaseTable(name: String) : IdTable<Uuid>(name) {
    override val id = uuid("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()
}