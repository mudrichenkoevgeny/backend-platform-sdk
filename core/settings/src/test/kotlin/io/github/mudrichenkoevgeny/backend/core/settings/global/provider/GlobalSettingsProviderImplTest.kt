package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class GlobalSettingsProviderImplTest {

    @Test
    fun `initialize registers defaults using config values or empty strings`() = runBlocking {
        val service = RecordingSettingsService()
        val config = SettingsConfig(
            privacyPolicyUrl = "privacy",
            termsOfServiceUrl = null,
            contactSupportEmail = "support@example.com"
        )
        val provider = GlobalSettingsProviderImpl(service, config)

        val result = provider.initialize()

        assertTrue(result is AppResult.Success)
        assertEquals(
            listOf(
                RegisterDefaultCall("global.privacy_policy_url", "privacy", SettingType.STRING),
                RegisterDefaultCall("global.terms_of_service_url", "", SettingType.STRING),
                RegisterDefaultCall("global.contact_support_email", "support@example.com", SettingType.STRING)
            ),
            service.registerDefaultCalls
        )
    }

    @Test
    fun `getSettings reads values from service and falls back to config`() {
        val service = RecordingSettingsService(
            stringByKey = mapOf(
                "global.privacy_policy_url" to "service_privacy",
                "global.contact_support_email" to "service_support@example.com"
            )
        )
        val config = SettingsConfig(
            privacyPolicyUrl = "config_privacy",
            termsOfServiceUrl = "config_tos",
            contactSupportEmail = "config_support@example.com"
        )
        val provider = GlobalSettingsProviderImpl(service, config)

        val result = provider.getSettings()

        assertEquals("service_privacy", result.privacyPolicyUrl)
        assertEquals("config_tos", result.termsOfServiceUrl)
        assertEquals("service_support@example.com", result.contactSupportEmail)
    }

    @Test
    fun `updateGlobalSettings delegates to service updateSetting for all keys`() = runBlocking {
        val service = RecordingSettingsService()
        val provider = GlobalSettingsProviderImpl(service, SettingsConfig(null, null, null))
        val payload = GlobalSettings(
            privacyPolicyUrl = "new_privacy",
            termsOfServiceUrl = "new_tos",
            contactSupportEmail = "new_support@example.com"
        )

        val result = provider.updateGlobalSettings(payload)

        assertTrue(result is AppResult.Success)
        assertEquals(
            listOf(
                UpdateSettingCall("global.privacy_policy_url", "new_privacy", SettingType.STRING),
                UpdateSettingCall("global.terms_of_service_url", "new_tos", SettingType.STRING),
                UpdateSettingCall("global.contact_support_email", "new_support@example.com", SettingType.STRING)
            ),
            service.updateSettingCalls
        )
    }

    @Test
    fun `updateGlobalSettings returns error when update fails`() = runBlocking {
        val error = CommonError.Database("fail")
        val service = RecordingSettingsService(
            failUpdateForKey = "global.terms_of_service_url",
            failUpdateError = error
        )
        val provider = GlobalSettingsProviderImpl(service, SettingsConfig(null, null, null))
        val payload = GlobalSettings("p", "t", "e")

        val result = provider.updateGlobalSettings(payload)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
        assertEquals(2, service.updateSettingCalls.size)
    }

    private data class RegisterDefaultCall(
        val key: String,
        val value: String,
        val type: SettingType
    )

    private data class UpdateSettingCall(
        val key: String,
        val value: String,
        val type: SettingType
    )

    private class RecordingSettingsService(
        private val stringByKey: Map<String, String?> = emptyMap(),
        private val failUpdateForKey: String? = null,
        private val failUpdateError: AppError? = null
    ) : SystemSettingsService {
        val registerDefaultCalls = mutableListOf<RegisterDefaultCall>()
        val updateSettingCalls = mutableListOf<UpdateSettingCall>()

        override suspend fun initialize(): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun registerDefault(key: String, value: String, type: SettingType): AppResult<Unit> {
            registerDefaultCalls += RegisterDefaultCall(key, value, type)
            return AppResult.Success(Unit)
        }

        override fun getString(key: String): String? = stringByKey[key]
        override fun getLong(key: String): Long? = null
        override fun getInt(key: String): Int? = null
        override fun getDouble(key: String): Double? = null
        override fun getBoolean(key: String): Boolean? = null
        override fun <T> getJson(key: String, deserializer: (String) -> T): T? = null

        override suspend fun updateSetting(
            key: String,
            value: String,
            type: SettingType
        ): AppResult<SystemSetting> {
            updateSettingCalls += UpdateSettingCall(key, value, type)
            if (key == failUpdateForKey && failUpdateError != null) {
                return AppResult.Error(failUpdateError)
            }
            return AppResult.Success(
                SystemSetting(
                    id = Uuid.random(),
                    key = key,
                    value = value,
                    type = type
                )
            )
        }

        override suspend fun deleteSetting(key: String): AppResult<Unit> = AppResult.Success(Unit)
    }
}