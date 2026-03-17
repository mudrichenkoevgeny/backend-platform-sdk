package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlobalSettingsProviderImplTest {

    @Test
    fun `initialize registers defaults only for non-null config values`() = runBlocking {
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
                RegisterDefaultCall(
                    key = "global.privacy_policy_url",
                    value = "privacy",
                    type = SettingType.STRING
                ),
                RegisterDefaultCall(
                    key = "global.contact_support_email",
                    value = "support@example.com",
                    type = SettingType.STRING
                )
            ),
            service.registerDefaultCalls
        )
    }

    @Test
    fun `getSettings reads values from service`() {
        val service = RecordingSettingsService(
            stringByKey = mapOf(
                "global.privacy_policy_url" to "privacy",
                "global.terms_of_service_url" to "tos",
                "global.contact_support_email" to "support@example.com"
            )
        )
        val provider = GlobalSettingsProviderImpl(service, SettingsConfig(null, null, null))

        val result = provider.getSettings()

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals("privacy", data.privacyPolicyUrl)
        assertEquals("tos", data.termsOfServiceUrl)
        assertEquals("support@example.com", data.contactSupportEmail)
    }

    @Test
    fun `updatePrivacyPolicyUrl delegates to service updateSetting and returns Unit`() = runBlocking {
        val service = RecordingSettingsService(
            updateSettingResult = AppResult.Success(Unit)
        )
        val provider = GlobalSettingsProviderImpl(service, SettingsConfig(null, null, null))

        val result = provider.updatePrivacyPolicyUrl("privacy")

        assertTrue(result is AppResult.Success)
        assertEquals(
            listOf(UpdateSettingCall("global.privacy_policy_url", "privacy", SettingType.STRING)),
            service.updateSettingCalls
        )
    }

    @Test
    fun `updateTermsOfServiceUrl returns error when service fails`() = runBlocking {
        val error = CommonError.Database("fail")
        val service = RecordingSettingsService(
            updateSettingResult = AppResult.Error(error)
        )
        val provider = GlobalSettingsProviderImpl(service, SettingsConfig(null, null, null))

        val result = provider.updateTermsOfServiceUrl("tos")

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
        assertEquals(
            listOf(UpdateSettingCall("global.terms_of_service_url", "tos", SettingType.STRING)),
            service.updateSettingCalls
        )
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
        private val updateSettingResult: AppResult<Unit> = AppResult.Success(Unit)
    ) : SystemSettingsService {
        val registerDefaultCalls = mutableListOf<RegisterDefaultCall>()
        val updateSettingCalls = mutableListOf<UpdateSettingCall>()

        override suspend fun initialize(): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun registerDefault(key: String, value: String, type: SettingType): AppResult<Unit> {
            registerDefaultCalls += RegisterDefaultCall(key, value, type)
            return AppResult.Success(Unit)
        }

        override fun getString(key: String): String? = stringByKey[key]

        override fun getLong(key: String): Long? = stringByKey[key]?.toLongOrNull()

        override fun getDouble(key: String): Double? = stringByKey[key]?.toDoubleOrNull()

        override fun getBoolean(key: String): Boolean? = stringByKey[key]?.toBooleanStrictOrNull()

        override fun <T> getJson(key: String, deserializer: (String) -> T): T? {
            val raw = stringByKey[key] ?: return null
            return try {
                deserializer(raw)
            } catch (_: Exception) {
                null
            }
        }

        override suspend fun updateSetting(
            key: String,
            value: String,
            type: SettingType
        ): AppResult<SystemSetting> {
            updateSettingCalls += UpdateSettingCall(key, value, type)
            return when (updateSettingResult) {
                is AppResult.Success -> AppResult.Success(
                    SystemSetting(
                        key = key,
                        value = value,
                        type = type
                    )
                )
                is AppResult.Error -> AppResult.Error(updateSettingResult.error)
            }
        }
    }
}

