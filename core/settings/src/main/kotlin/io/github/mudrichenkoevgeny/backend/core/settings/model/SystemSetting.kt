package io.github.mudrichenkoevgeny.backend.core.settings.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class SystemSetting(
    val id: Uuid = Uuid.random(),
    val key: String,
    val value: String,
    val type: SettingType,
    val description: String? = null
)