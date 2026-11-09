package io.github.mudrichenkoevgeny.backend.core.settings.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import org.jetbrains.exposed.v1.core.Column

/**
 * Exposed table mapping for [SystemSetting].
 * Inherits id, createdAt, updatedAt from [BaseTable].
 *
 * Schema is created by a Flyway migration in `db/migration/core/settings/`.
 * The app must include this path in its Flyway migration locations.
 */
public object SystemSettingsTable : BaseTable("system_settings") {
    public val key: Column<String> = varchar(
        "key",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    )
    public val value: Column<String> = text("value")
    public val type: Column<SettingType> = enumerationByName(
        "type",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        SettingType::class
    )
    public val description: Column<String?> = text("description").nullable()
}
