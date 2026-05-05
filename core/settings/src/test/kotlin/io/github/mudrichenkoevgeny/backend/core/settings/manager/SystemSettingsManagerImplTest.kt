package io.github.mudrichenkoevgeny.backend.core.settings.manager

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.database.repository.SystemSettingRepository
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class SystemSettingsManagerImplTest {

    private val repository = mockk<SystemSettingRepository>()
    private val manager = SystemSettingsManagerImpl(repository)

    @Test
    fun `saveSetting delegates to repository`() = runBlocking {
        val setting = SystemSetting(
            id = Uuid.random(),
            key = "test.key",
            value = "value",
            type = SettingType.STRING
        )
        coEvery { repository.saveSetting(setting) } returns AppResult.Success(setting)

        val result = manager.saveSetting(setting)

        assertTrue(result is AppResult.Success)
        assertEquals(setting, (result as AppResult.Success).data)
    }

    @Test
    fun `getSettingByKey delegates to repository`() = runBlocking {
        val key = "test.key"
        val setting = SystemSetting(
            id = Uuid.random(),
            key = key,
            value = "value",
            type = SettingType.STRING
        )
        coEvery { repository.getSettingByKey(key) } returns AppResult.Success(setting)

        val result = manager.getSettingByKey(key)

        assertTrue(result is AppResult.Success)
        assertEquals(setting, (result as AppResult.Success).data)
    }

    @Test
    fun `getAllSettings delegates to repository`() = runBlocking {
        val settings = listOf(
            SystemSetting(Uuid.random(), "k1", "v1", SettingType.STRING),
            SystemSetting(Uuid.random(), "k2", "v2", SettingType.STRING)
        )
        coEvery { repository.getAllSettings() } returns AppResult.Success(settings)

        val result = manager.getAllSettings()

        assertTrue(result is AppResult.Success)
        assertEquals(settings, (result as AppResult.Success).data)
    }

    @Test
    fun `deleteSetting delegates to repository`() = runBlocking {
        val key = "test.key"
        coEvery { repository.deleteSetting(key) } returns AppResult.Success(Unit)

        val result = manager.deleteSetting(key)

        assertTrue(result is AppResult.Success)
    }
}