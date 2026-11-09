package io.github.mudrichenkoevgeny.backend.core.settings.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.database.table.SystemSettingsTable
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposed-based implementation of [SystemSettingsRepository] operating on [SystemSettingsTable].
 *
 * Notes:
 * - Uses `upsert` by [SystemSettingsTable.key] to make [saveSetting] idempotent per key.
 * - Does not wrap queries into a transaction itself; callers are expected to execute it within
 *   their DB context (e.g. via `dbQuery`).
 */
@Singleton
public class SystemSettingsRepositoryImpl @Inject constructor(
    private val clock: Clock = Clock.systemUTC()
) : SystemSettingsRepository {
    override suspend fun saveSettings(settings: List<SystemSetting>): AppResult<List<SystemSetting>> {
        if (settings.isEmpty()) {
            return AppResult.Success(emptyList())
        }

        return try {
            val now = Instant.now(clock)

            SystemSettingsTable.batchUpsert(
                data = settings,
                keys = arrayOf(SystemSettingsTable.key)
            ) { setting ->
                this[SystemSettingsTable.id] = setting.id
                this[SystemSettingsTable.key] = setting.key
                this[SystemSettingsTable.value] = setting.value
                this[SystemSettingsTable.type] = setting.type
                this[SystemSettingsTable.description] = setting.description
                this[SystemSettingsTable.updatedAt] = now
            }

            AppResult.Success(settings)
        } catch (e: ExposedSQLException) {
            AppResult.Error(
                CommonError.Database("Database error while saving settings batch: ${e.message}")
            )
        }
    }

    override suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting> = try {
        val now = Instant.now(clock)

        SystemSettingsTable.upsert(SystemSettingsTable.key) { row ->
            row[id] = setting.id
            row[key] = setting.key
            row[value] = setting.value
            row[type] = setting.type
            row[description] = setting.description
            row[updatedAt] = now
        }

        AppResult.Success(setting)
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Database error while saving setting ${setting.key}: ${e.message}")
        )
    }

    override suspend fun getSettingByKey(key: String): AppResult<SystemSetting?> = try {
        val resultRow = SystemSettingsTable.selectAll().where { SystemSettingsTable.key eq key }.singleOrNull()

        AppResult.Success(resultRow?.toSystemSetting())
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Database error while retrieving setting $key: ${e.message}")
        )
    }

    override suspend fun getAllSettings(): AppResult<List<SystemSetting>> = try {
        val settings = SystemSettingsTable.selectAll().map { resultRow -> resultRow.toSystemSetting() }

        AppResult.Success(settings)
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Database error while retrieving all settings: ${e.message}")
        )
    }

    override suspend fun deleteSetting(key: String): AppResult<Unit> = try {
        SystemSettingsTable.deleteWhere { SystemSettingsTable.key eq key }
        AppResult.Success(Unit)
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Database error while deleting setting $key: ${e.message}")
        )
    }

    private fun ResultRow.toSystemSetting(): SystemSetting = SystemSetting(
        id = this[SystemSettingsTable.id].value,
        key = this[SystemSettingsTable.key],
        value = this[SystemSettingsTable.value],
        type = this[SystemSettingsTable.type],
        description = this[SystemSettingsTable.description]
    )
}
