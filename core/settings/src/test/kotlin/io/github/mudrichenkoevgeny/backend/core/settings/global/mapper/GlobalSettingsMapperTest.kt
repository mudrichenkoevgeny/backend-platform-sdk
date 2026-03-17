package io.github.mudrichenkoevgeny.backend.core.settings.global.mapper

import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GlobalSettingsMapperTest {

    @Test
    fun `toGlobalSettingsResponse maps fields`() {
        val model = GlobalSettings(
            privacyPolicyUrl = "privacy",
            termsOfServiceUrl = "tos",
            contactSupportEmail = "support@example.com"
        )

        val response = model.toGlobalSettingsResponse()

        assertEquals("privacy", response.privacyPolicyUrl)
        assertEquals("tos", response.termsOfServiceUrl)
        assertEquals("support@example.com", response.contactSupportEmail)
    }
}

