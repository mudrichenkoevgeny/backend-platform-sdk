package io.github.mudrichenkoevgeny.backend.core.settings.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.settings.database.table.SystemSettingsTable
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemSettingsRepositoryImplTest {
    private val dataSource = createTestDataSource("settings_repo")
    private lateinit var repository: SystemSettingsRepository

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.create(SystemSettingsTable)
            }
        }
        repository = SystemSettingsRepositoryImpl()
    }

    @Test
    fun `saveSetting persists data and getSettingByKey returns it`() = runBlocking {
        val setting = SystemSetting(
            key = "app.theme",
            value = "dark",
            type = SettingType.STRING,
            description = "Main application theme"
        )

        suspendTransaction { repository.saveSetting(setting) }
        val result = suspendTransaction { repository.getSettingByKey(setting.key) }

        val success = result as AppResult.Success
        val data = checkNotNull(success.data)
        assertEquals(
            setting.key,
            data.key
        )
        assertEquals(
            setting.value,
            data.value
        )
        assertEquals(
            setting.type,
            data.type
        )
    }

    @Test
    fun `saveSetting is idempotent using upsert by key`() = runBlocking {
        val key = "app.maintenance"
        val settingV1 = SystemSetting(
            key = key,
            value = "false",
            type = SettingType.BOOLEAN,
            description = "Initial status"
        )
        val settingV2 = settingV1.copy(
            value = "true",
            description = "Updated status"
        )

        suspendTransaction { repository.saveSetting(settingV1) }
        suspendTransaction { repository.saveSetting(settingV2) }

        val result = suspendTransaction { repository.getSettingByKey(key) }
        val success = result as AppResult.Success
        val data = checkNotNull(success.data)

        assertEquals(
            "true",
            data.value
        )
        assertEquals(
            "Updated status",
            data.description
        )

        val allSettings = suspendTransaction { repository.getAllSettings() } as AppResult.Success
        assertEquals(
            1,
            allSettings.data.count { it.key == key }
        )
    }

    @Test
    fun `getSettingByKey returns null when key does not exist`() = runBlocking {
        val result = suspendTransaction { repository.getSettingByKey("non.existent.key") }

        val success = result as AppResult.Success
        assertNull(success.data)
    }

    @Test
    fun `saveSettings batch persists all settings`() = runBlocking {
        val s1 = SystemSetting(
            key = "batch.key.1",
            value = "val1",
            type = SettingType.STRING
        )
        val s2 = SystemSetting(
            key = "batch.key.2",
            value = "val2",
            type = SettingType.STRING
        )

        val saveResult = suspendTransaction {
            repository.saveSettings(
                listOf(
                    s1,
                    s2
                )
            )
        }
        assertTrue(saveResult is AppResult.Success)

        val check1 = suspendTransaction { repository.getSettingByKey("batch.key.1") } as AppResult.Success
        val check2 = suspendTransaction { repository.getSettingByKey("batch.key.2") } as AppResult.Success

        val data1 = checkNotNull(check1.data)
        val data2 = checkNotNull(check2.data)
        assertEquals(
            "val1",
            data1.value
        )
        assertEquals(
            "val2",
            data2.value
        )
    }

    @Test
    fun `getAllSettings returns all persisted settings`() = runBlocking {
        val setting1 = SystemSetting(
            key = "key.1",
            value = "val1",
            type = SettingType.STRING
        )
        val setting2 = SystemSetting(
            key = "key.2",
            value = "val2",
            type = SettingType.STRING
        )

        suspendTransaction {
            repository.saveSetting(setting1)
            repository.saveSetting(setting2)
        }

        val result = suspendTransaction { repository.getAllSettings() }
        val success = result as AppResult.Success

        assertTrue(success.data.any { it.key == "key.1" })
        assertTrue(success.data.any { it.key == "key.2" })
    }

    @Test
    fun `deleteSetting removes setting by key`() = runBlocking {
        val key = "temporary.key"
        val setting = SystemSetting(
            key = key,
            value = "temp",
            type = SettingType.STRING
        )

        suspendTransaction { repository.saveSetting(setting) }

        val deleteResult = suspendTransaction { repository.deleteSetting(key) }
        assertTrue(deleteResult is AppResult.Success)

        val checkResult = suspendTransaction { repository.getSettingByKey(key) }
        val success = checkResult as AppResult.Success
        assertNull(success.data)
    }
}
