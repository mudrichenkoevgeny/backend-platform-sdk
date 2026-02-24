package io.github.mudrichenkoevgeny.backend.core.settings.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.database.table.SystemSettingsTable
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Singleton
class SystemSettingRepositoryImpl @Inject constructor() : SystemSettingRepository {

    override suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting> {
        return try {
            val now = Instant.now()

            SystemSettingsTable.upsert { row ->
                setting.id?.let { row[id] = it }
                row[key] = setting.key
                row[value] = setting.value
                row[type] = setting.type
                row[description] = setting.description
                row[updatedAt] = now
            }

            AppResult.Success(setting)
        } catch (e: Exception) {
            AppResult.Error(
                CommonError.Database("Database error while saving setting ${setting.key}: ${e.message}")
            )
        }
    }

    override suspend fun getSettingByKey(key: String): AppResult<SystemSetting?> {
        val resultRow = SystemSettingsTable
            .selectAll()
            .where { SystemSettingsTable.key eq key }
            .singleOrNull()

        return AppResult.Success(resultRow?.toSystemSetting())
    }

    override suspend fun getAllSettings(): AppResult<List<SystemSetting>> {
        val settings = SystemSettingsTable
            .selectAll()
            .map { it.toSystemSetting() }

        return AppResult.Success(settings)
    }

    private fun ResultRow.toSystemSetting(): SystemSetting = SystemSetting(
        id = this[SystemSettingsTable.id].value,
        key = this[SystemSettingsTable.key],
        value = this[SystemSettingsTable.value],
        type = this[SystemSettingsTable.type],
        description = this[SystemSettingsTable.description]
    )
}