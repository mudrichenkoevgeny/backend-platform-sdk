package io.github.mudrichenkoevgeny.backend.core.database.table

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

open class BaseTable(name: String) : IdTable<UUID>(name) {
    override val id = uuid("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()
}