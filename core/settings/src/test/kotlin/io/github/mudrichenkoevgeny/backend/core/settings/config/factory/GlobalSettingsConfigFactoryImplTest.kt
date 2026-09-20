package io.github.mudrichenkoevgeny.backend.core.settings.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.envkeys.SettingsEnvKeys
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlobalSettingsConfigFactoryImplTest {

    private val envReader = mockk<EnvReader>()

    @Test
    fun `create builds GlobalSettingsConfig from env values`() {
        every { envReader.getByKeyOrNull(SettingsEnvKeys.PRIVACY_POLICY_URL) } returns "https://example.com/privacy"
        every { envReader.getByKeyOrNull(SettingsEnvKeys.TERMS_OF_SERVICE_URL) } returns "https://example.com/terms"
        every { envReader.getByKeyOrNull(SettingsEnvKeys.CONTACT_SUPPORT_EMAIL) } returns "support@example.com"
        every { envReader.getByKeyOrNull(SettingsEnvKeys.MIN_SUPPORTED_APP_VERSIONS) } returns """{"WEB":"1.0.0"}"""
        every { envReader.getByKeyOrNull(SettingsEnvKeys.IS_TRACING_ENABLED) } returns "true"
        every { envReader.getByKeyOrNull(SettingsEnvKeys.IS_METRICS_ENABLED) } returns "true"
        every { envReader.getByKeyOrNull(SettingsEnvKeys.IS_VERBOSE_LOGGING_ENABLED) } returns "false"

        val factory = GlobalSettingsConfigFactoryImpl(envReader)
        val config = factory.create()

        val settings = config.managementGlobalSettings
        assertEquals("https://example.com/privacy", settings.privacyPolicyUrl)
        assertEquals("https://example.com/terms", settings.termsOfServiceUrl)
        assertEquals("support@example.com", settings.contactSupportEmail)
        assertEquals("1.0.0", settings.minSupportedAppVersions.values.firstOrNull())
        assertTrue(settings.isTracingEnabled)
        assertTrue(settings.isMetricsEnabled)
        assertFalse(settings.isVerboseLoggingEnabled)
    }

    @Test
    fun `create falls back to defaults when env values missing`() {
        every { envReader.getByKeyOrNull(any()) } returns null

        val factory = GlobalSettingsConfigFactoryImpl(envReader)
        val config = factory.create()

        val settings = config.managementGlobalSettings
        assertNull(settings.privacyPolicyUrl)
        assertNull(settings.termsOfServiceUrl)
        assertNull(settings.contactSupportEmail)
        assertTrue(settings.minSupportedAppVersions.isEmpty())
        assertFalse(settings.isTracingEnabled)
        assertFalse(settings.isMetricsEnabled)
        assertFalse(settings.isVerboseLoggingEnabled)
    }
}
