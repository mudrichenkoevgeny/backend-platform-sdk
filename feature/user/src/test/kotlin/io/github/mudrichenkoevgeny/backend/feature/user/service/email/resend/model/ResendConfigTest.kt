package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ResendConfigTest {

    @Test
    fun `createOrNull returns null when any parameter blank`() {
        assertNull(ResendConfig.createOrNull(null, URL, FROM_EMAIL, FROM_NAME))
        assertNull(ResendConfig.createOrNull(API_KEY, "", FROM_EMAIL, FROM_NAME))
        assertNull(ResendConfig.createOrNull(API_KEY, URL, " ", FROM_NAME))
        assertNull(ResendConfig.createOrNull(API_KEY, URL, FROM_EMAIL, null))
    }

    @Test
    fun `createOrNull returns config when all parameters present`() {
        val config = ResendConfig.createOrNull(API_KEY, URL, FROM_EMAIL, FROM_NAME)

        assertNotNull(config)
        config as ResendConfig

        assertEquals(API_KEY, config.apiKey)
        assertEquals(URL, config.url)
        assertEquals(FROM_EMAIL, config.fromEmail)
        assertEquals(FROM_NAME, config.fromName)
    }

    private companion object {
        const val API_KEY = "key"
        const val URL = "https://api.example.com"
        const val FROM_EMAIL = "noreply@example.com"
        const val FROM_NAME = "Example"
    }
}

