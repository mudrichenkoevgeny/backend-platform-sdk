package io.github.mudrichenkoevgeny.backend.core.settings.model

import kotlin.uuid.Uuid

/**
 * A single DB-backed system setting entry.
 *
 * The value is stored as a string, while [type] describes the intended interpretation/parsing.
 *
 * @property id unique identifier of the setting row
 * @property key stable setting key (unique within the settings table)
 * @property value raw string value stored in the database
 * @property type describes how [value] should be interpreted by consumers
 * @property description optional human-readable description of the setting
 */
data class SystemSetting(
    val id: Uuid = Uuid.random(),
    val key: String,
    val value: String,
    val type: SettingType,
    val description: String? = null
)