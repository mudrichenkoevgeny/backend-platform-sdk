package io.github.mudrichenkoevgeny.backend.core.settings.service

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.settings.manager.RecordingSystemSettingsManager
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
        val manager = RecordingSystemSettingsManager(
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
        val manager = RecordingSystemSettingsManager(
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
        val manager = RecordingSystemSettingsManager(
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
        val manager = RecordingSystemSettingsManager(
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
        val manager = RecordingSystemSettingsManager(
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
        val manager = RecordingSystemSettingsManager(
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
        val manager = RecordingSystemSettingsManager(
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
        val manager = RecordingSystemSettingsManager(
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
    fun `registerDefaults batch saves missing keys`() = runTest {
        val existing = SystemSetting(key = "k1", value = "v1", type = SettingType.STRING)
        val newSetting = SystemSetting(key = "k2", value = "v2", type = SettingType.STRING)
        val manager = RecordingSystemSettingsManager(
            getAllSettingsResult = AppResult.Success(listOf(existing))
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.registerDefaults(listOf(existing, newSetting))

        assertTrue(result is AppResult.Success)
        assertEquals(1, manager.saveCalls.size)
        assertEquals("k2", manager.saveCalls.single().key)
        assertEquals("v2", service.getString("k2"))
    }

    @Test
    fun `updateSettings batch updates all keys`() = runTest {
        val s1 = SystemSetting(key = "k1", value = "v1", type = SettingType.STRING)
        val s2 = SystemSetting(key = "k2", value = "v2", type = SettingType.STRING)
        val manager = RecordingSystemSettingsManager(
            getAllSettingsResult = AppResult.Success(emptyList())
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.updateSettings(listOf(s1, s2))

        assertTrue(result is AppResult.Success)
        assertEquals(2, manager.saveCalls.size)
        assertEquals("v1", service.getString("k1"))
        assertEquals("v2", service.getString("k2"))
    }

    @Test
    fun `deleteSetting removes from cache and manager`() = runTest {
        val existing = SystemSetting(key = "k", value = "v", type = SettingType.STRING)
        val manager = RecordingSystemSettingsManager(
            getAllSettingsResult = AppResult.Success(listOf(existing))
        )
        val service = SystemSettingsServiceImpl(manager, redisManager, scope)
        service.initialize()

        val result = service.deleteSetting("k")

        assertTrue(result is AppResult.Success)
        assertNull(service.getString("k"))
        assertEquals("k", manager.deleteCalls.single())
    }
}
