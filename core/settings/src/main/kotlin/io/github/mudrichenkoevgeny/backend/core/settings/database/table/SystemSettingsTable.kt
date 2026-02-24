package io.github.mudrichenkoevgeny.backend.core.settings.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object SystemSettingsTable : BaseTable("system_settings") {
    val key = varchar("key", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val value = text("value")
    val type = enumerationByName("type", BaseDbConstraints.ENUM_MAX_LENGTH, SettingType::class)
    val description = text("description").nullable()
}