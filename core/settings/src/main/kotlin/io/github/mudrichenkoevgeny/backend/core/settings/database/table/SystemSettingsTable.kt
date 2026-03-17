package io.github.mudrichenkoevgeny.backend.core.settings.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType

/**
 * Exposed table mapping for [SystemSetting].
 * Inherits id, createdAt, updatedAt from [BaseTable].
 *
 * Schema is created by a Flyway migration in `db/migration/core/settings/`.
 * The app must include this path in its Flyway migration locations.
 */
object SystemSettingsTable : BaseTable("system_settings") {
    val key = varchar("key", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val value = text("value")
    val type = enumerationByName("type", BaseDbConstraints.ENUM_MAX_LENGTH, SettingType::class)
    val description = text("description").nullable()
}