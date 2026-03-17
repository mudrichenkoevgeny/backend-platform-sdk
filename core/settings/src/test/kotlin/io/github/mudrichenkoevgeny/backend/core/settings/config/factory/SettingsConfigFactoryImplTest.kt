package io.github.mudrichenkoevgeny.backend.core.settings.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.envkeys.SettingsEnvKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk

class SettingsConfigFactoryImplTest {

    @Test
    fun `create returns values from EnvReader`() {
        val envReader = mockk<EnvReader>()
        every { envReader.getByKeyOrNull(SettingsEnvKeys.PRIVACY_POLICY_URL) } returns "https://example.com/privacy"
        every { envReader.getByKeyOrNull(SettingsEnvKeys.TERMS_OF_SERVICE_URL) } returns "https://example.com/tos"
        every { envReader.getByKeyOrNull(SettingsEnvKeys.CONTACT_SUPPORT_EMAIL) } returns "support@example.com"

        val factory = SettingsConfigFactoryImpl(envReader)

        val config = factory.create()

        assertEquals("https://example.com/privacy", config.privacyPolicyUrl)
        assertEquals("https://example.com/tos", config.termsOfServiceUrl)
        assertEquals("support@example.com", config.contactSupportEmail)
    }

    @Test
    fun `create returns nulls when variables are missing`() {
        val envReader = mockk<EnvReader>()
        every { envReader.getByKeyOrNull(any()) } returns null

        val factory = SettingsConfigFactoryImpl(envReader)

        val config = factory.create()

        assertEquals(null, config.privacyPolicyUrl)
        assertEquals(null, config.termsOfServiceUrl)
        assertEquals(null, config.contactSupportEmail)
    }
}

