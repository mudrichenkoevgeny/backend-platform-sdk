package io.github.mudrichenkoevgeny.backend.core.settings.service

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.settings.manager.SystemSettingsManager
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemSettingsServiceImplTest {

    private val redisManager = mockk<RedisManager>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun `initialize populates cache from manager`() = runTest {
        val existing = listOf(
            SystemSetting(key = "a", value = "1", type = SettingType.LONG),
            SystemSetting(key = "b", value = "true", type = SettingType.BOOLEAN)
        )
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(existing)
        )

        val service = SystemSettingsServiceImpl(manager, redisManager, scope)

        val result = service.initialize()

        assertTrue(result is AppResult.Success)
        assertEquals("1", service.getString("a"))
        assertEquals(true, service.getBoolean("b"))
    }

    @Test
    fun `initialize returns error when manager fails`() = runTest {
        val error = CommonError.Database("boom")
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Error(error)
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)

        val result = service.initialize()

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `registerDefault does nothing when key already cached`() = runTest {
        val cachedSetting = SystemSetting(key = "k", value = "v", type = SettingType.STRING)
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(listOf(cachedSetting)),
            saveSettingResult = AppResult.Success(cachedSetting)
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.registerDefault("k", "new", SettingType.STRING)

        assertTrue(result is AppResult.Success)
        assertEquals(0, manager.saveCalls.size)
        assertEquals("v", service.getString("k"))
    }

    @Test
    fun `registerDefault persists and caches when key missing`() = runTest {
        val saved = SystemSetting(key = "k", value = "v", type = SettingType.STRING)
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(emptyList()),
            saveSettingResult = AppResult.Success(saved)
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.registerDefault("k", "v", SettingType.STRING)

        assertTrue(result is AppResult.Success)
        assertEquals(1, manager.saveCalls.size)
        assertEquals("k", manager.saveCalls.single().key)
        assertEquals("v", service.getString("k"))
    }

    @Test
    fun `typed getters return null when value is missing or invalid`() = runTest {
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(
                listOf(
                    SystemSetting(key = "long", value = "x", type = SettingType.LONG),
                    SystemSetting(key = "double", value = "x", type = SettingType.DOUBLE),
                    SystemSetting(key = "bool", value = "yes", type = SettingType.BOOLEAN)
                )
            )
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        assertNull(service.getLong("missing"))
        assertNull(service.getLong("long"))
        assertNull(service.getDouble("double"))
        assertNull(service.getBoolean("bool"))
    }

    @Test
    fun `getJson returns deserialized value and returns null on deserializer error`() = runTest {
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(
                listOf(SystemSetting(key = "json", value = """{"a":1}""", type = SettingType.JSON))
            )
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val ok = service.getJson("json") { text -> text.length }
        val bad = service.getJson("json") { _ -> error("parse failed") }

        assertEquals("""{"a":1}""".length, ok)
        assertNull(bad)
    }

    @Test
    fun `updateSetting creates new setting when missing and caches result`() = runTest {
        val saved = SystemSetting(key = "k", value = "v1", type = SettingType.STRING)
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(emptyList()),
            saveSettingResult = AppResult.Success(saved)
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.updateSetting("k", "v1", SettingType.STRING)

        assertTrue(result is AppResult.Success)
        assertEquals("v1", service.getString("k"))
        assertEquals(1, manager.saveCalls.size)
    }

    @Test
    fun `updateSetting keeps existing type and id when present`() = runTest {
        val existing = SystemSetting(key = "k", value = "v0", type = SettingType.LONG)
        val saved = existing.copy(value = "v1")
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(listOf(existing)),
            saveSettingResult = AppResult.Success(saved)
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.updateSetting("k", "v1", SettingType.STRING)

        assertTrue(result is AppResult.Success)
        assertEquals(existing.id, (result as AppResult.Success).data.id)
        assertEquals(SettingType.LONG, result.data.type)
        assertEquals("v1", service.getString("k"))
    }

    @Test
    fun `deleteSetting removes from cache and manager`() = runTest {
        val existing = SystemSetting(key = "k", value = "v", type = SettingType.STRING)
        val manager = RecordingManager(
            getAllSettingsResult = AppResult.Success(listOf(existing))
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.deleteSetting("k")

        assertTrue(result is AppResult.Success)
        assertNull(service.getString("k"))
        assertEquals("k", manager.deleteCalls.single())
    }

    private class RecordingManager(
        private val getAllSettingsResult: AppResult<List<SystemSetting>> = AppResult.Success(emptyList()),
        private val saveSettingResult: AppResult<SystemSetting> = AppResult.Success(
            SystemSetting(key = "unused", value = "unused", type = SettingType.STRING)
        )
    ) : SystemSettingsManager {
        val saveCalls = mutableListOf<SystemSetting>()
        val deleteCalls = mutableListOf<String>()

        override suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting> {
            saveCalls += setting
            return saveSettingResult
        }

        override suspend fun getSettingByKey(key: String): AppResult<SystemSetting?> = AppResult.Success(null)

        override suspend fun getAllSettings(): AppResult<List<SystemSetting>> = getAllSettingsResult

        override suspend fun deleteSetting(key: String): AppResult<Unit> {
            deleteCalls += key
            return AppResult.Success(Unit)
        }
    }
}