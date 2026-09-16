package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.GlobalSettingsConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.TestSystemSettingsService
import io.github.mudrichenkoevgeny.backend.core.settings.service.RegisterDefaultCall
import io.github.mudrichenkoevgeny.backend.core.settings.service.UpdateSettingCall
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlobalSettingsProviderImplTest {

    @Test
    fun `initialize registers defaults using config values or empty strings`() = runBlocking {
        val service = TestSystemSettingsService()
        val config = GlobalSettingsConfig(
            privacyPolicyUrl = "privacy",
            termsOfServiceUrl = null,
            contactSupportEmail = "support@example.com",
            minSupportedAppVersions = emptyMap(),
            isTracingEnabled = true,
            isMetricsEnabled = false,
            isVerboseLoggingEnabled = false
        )
        val provider = GlobalSettingsProviderImpl(service, config)

        val result = provider.initialize()

        assertTrue(result is AppResult.Success)
        assertEquals(
            listOf(
                RegisterDefaultCall("global.privacy_policy_url", "privacy", SettingType.STRING),
                RegisterDefaultCall("global.terms_of_service_url", "", SettingType.STRING),
                RegisterDefaultCall("global.contact_support_email", "support@example.com", SettingType.STRING),
                RegisterDefaultCall("global.min_supported_app_versions", "{}", SettingType.JSON),
                RegisterDefaultCall("global.is_tracing_enabled", "true", SettingType.BOOLEAN),
                RegisterDefaultCall("global.is_metrics_enabled", "false", SettingType.BOOLEAN),
                RegisterDefaultCall("global.is_verbose_logging_enabled", "false", SettingType.BOOLEAN)
            ),
            service.registerDefaultCalls
        )
    }

    @Test
    fun `getOpenGlobalSettings reads values from service and falls back to config`() {
        val service = TestSystemSettingsService(
            stringByKey = mapOf(
                "global.privacy_policy_url" to "service_privacy",
                "global.contact_support_email" to "service_support@example.com"
            )
        )
        val config = GlobalSettingsConfig(
            privacyPolicyUrl = "config_privacy",
            termsOfServiceUrl = "config_tos",
            contactSupportEmail = "config_support@example.com"
        )
        val provider = GlobalSettingsProviderImpl(service, config)

        val result = provider.getOpenGlobalSettings()

        assertEquals("service_privacy", result.privacyPolicyUrl)
        assertEquals("config_tos", result.termsOfServiceUrl)
        assertEquals("service_support@example.com", result.contactSupportEmail)
    }

    @Test
    fun `getManagementGlobalSettings reads boolean values and flags`() {
        val service = TestSystemSettingsService(
            booleanByKey = mapOf(
                "global.is_tracing_enabled" to true,
                "global.is_metrics_enabled" to false,
                "global.is_verbose_logging_enabled" to true
            )
        )
        val config = GlobalSettingsConfig(null, null, null)
        val provider = GlobalSettingsProviderImpl(service, config)

        val result = provider.getManagementGlobalSettings()

        assertTrue(result.isTracingEnabled)
        assertFalse(result.isMetricsEnabled)
        assertTrue(result.isVerboseLoggingEnabled)
    }

    @Test
    fun `updateManagementGlobalSettings delegates to service updateSettings for all keys`() = runBlocking {
        val service = TestSystemSettingsService()
        val provider = GlobalSettingsProviderImpl(service, GlobalSettingsConfig(null, null, null))
        val payload = ManagementGlobalSettings(
            privacyPolicyUrl = "new_privacy",
            termsOfServiceUrl = "new_tos",
            contactSupportEmail = "new_support@example.com",
            maintenanceUntilEpochMillis = null,
            minSupportedAppVersions = emptyMap(),
            isTracingEnabled = true,
            isMetricsEnabled = true,
            isVerboseLoggingEnabled = false
        )

        val result = provider.updateManagementGlobalSettings(payload)

        assertTrue(result is AppResult.Success)
        assertEquals(
            listOf(
                UpdateSettingCall("global.privacy_policy_url", "new_privacy", SettingType.STRING),
                UpdateSettingCall("global.terms_of_service_url", "new_tos", SettingType.STRING),
                UpdateSettingCall("global.contact_support_email", "new_support@example.com", SettingType.STRING),
                UpdateSettingCall("global.min_supported_app_versions", "{}", SettingType.JSON),
                UpdateSettingCall("global.is_tracing_enabled", "true", SettingType.BOOLEAN),
                UpdateSettingCall("global.is_metrics_enabled", "true", SettingType.BOOLEAN),
                UpdateSettingCall("global.is_verbose_logging_enabled", "false", SettingType.BOOLEAN)
            ),
            service.updateSettingCalls
        )
    }

    @Test
    fun `updateManagementGlobalSettings returns error when update fails`() = runBlocking {
        val error = CommonError.Database("fail")
        val service = TestSystemSettingsService(
            failUpdateForKey = "global.terms_of_service_url",
            failUpdateError = error
        )
        val provider = GlobalSettingsProviderImpl(service, GlobalSettingsConfig(null, null, null))
        val payload = ManagementGlobalSettings(
            privacyPolicyUrl = "p",
            termsOfServiceUrl = "t",
            contactSupportEmail = "e",
            maintenanceUntilEpochMillis = null,
            minSupportedAppVersions = emptyMap(),
            isTracingEnabled = true,
            isMetricsEnabled = true,
            isVerboseLoggingEnabled = false
        )

        val result = provider.updateManagementGlobalSettings(payload)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
        assertEquals(2, service.updateSettingCalls.size)
    }
}
