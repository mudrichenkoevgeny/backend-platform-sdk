package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UniOneConfigTest {

    @Test
    fun `createOrNull returns null when any parameter blank`() {
        assertNull(UniOneConfig.createOrNull(null, URL, FROM_EMAIL, FROM_NAME, TRACK_DOMAIN, API_SEND))
        assertNull(UniOneConfig.createOrNull(API_KEY, " ", FROM_EMAIL, FROM_NAME, TRACK_DOMAIN, API_SEND))
        assertNull(UniOneConfig.createOrNull(API_KEY, URL, "", FROM_NAME, TRACK_DOMAIN, API_SEND))
        assertNull(UniOneConfig.createOrNull(API_KEY, URL, FROM_EMAIL, null, TRACK_DOMAIN, API_SEND))
        assertNull(UniOneConfig.createOrNull(API_KEY, URL, FROM_EMAIL, FROM_NAME, "", API_SEND))
        assertNull(UniOneConfig.createOrNull(API_KEY, URL, FROM_EMAIL, FROM_NAME, TRACK_DOMAIN, " "))
    }

    @Test
    fun `createOrNull returns config when all parameters present`() {
        val config = UniOneConfig.createOrNull(API_KEY, URL, FROM_EMAIL, FROM_NAME, TRACK_DOMAIN, API_SEND)

        assertNotNull(config)
        config as UniOneConfig

        assertEquals(API_KEY, config.apiKey)
        assertEquals(URL, config.url)
        assertEquals(FROM_EMAIL, config.fromEmail)
        assertEquals(FROM_NAME, config.fromName)
        assertEquals(TRACK_DOMAIN, config.trackDomain)
        assertEquals(API_SEND, config.apiSend)
    }

    private companion object {
        const val API_KEY = "key"
        const val URL = "https://api.example.com"
        const val FROM_EMAIL = "noreply@example.com"
        const val FROM_NAME = "Example"
        const val TRACK_DOMAIN = "trk.example.com"
        const val API_SEND = "api/send"
    }
}

