package io.github.mudrichenkoevgeny.backend.feature.user.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.feature.user.config.envkeys.UserEnvKeys
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model.ResendConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserConfigFactoryImplTest {

    private val envReader: EnvReader = mockk()
    private val factory = UserConfigFactoryImpl(envReader = envReader)

    @Test
    fun `create builds config and leaves email providers null when missing`() {
        stubBaseEnv(
            availablePrimary = "GOOGLE",
            availableSecondary = "EMAIL"
        )

        every { envReader.getByKey(UserEnvKeys.GOOGLE_WEB_CLIENT_ID) } returns "google-client-id"

        // UniOne - missing (blank)
        every { envReader.getByKey(UserEnvKeys.UNIONE_API_KEY_FILE) } returns "unione.key"
        every { envReader.readSecret("unione.key") } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_URL) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_EMAIL) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_NAME) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_TRACK_DOMAIN) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_API_SEND) } returns ""

        // Resend - missing (blank)
        every { envReader.getByKey(UserEnvKeys.RESEND_API_KEY_FILE) } returns "resend.key"
        every { envReader.readSecret("resend.key") } returns ""
        every { envReader.getByKey(UserEnvKeys.RESEND_URL) } returns ""
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_EMAIL) } returns ""
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_NAME) } returns ""

        val config = factory.create()

        assertEquals("jwt-secret", config.jwtSecret)
        assertEquals(12L, config.accessTokenValidityHours)
        assertEquals(30L, config.refreshTokenValidityDays)
        assertEquals("realm", config.authRealm)
        assertEquals(1, config.adminAccountsList.size)

        assertEquals("google-client-id", config.googleWebClientId)
        assertNull(config.uniOneConfig)
        assertNull(config.resendConfig)

        assertEquals(listOf(UserAuthProvider.GOOGLE), config.authSettings.availableAuthProviders.primary)
        assertEquals(listOf(UserAuthProvider.EMAIL), config.authSettings.availableAuthProviders.secondary)
    }

    @Test
    fun `create builds UniOne and Resend configs when all values present`() {
        stubBaseEnv(
            availablePrimary = "GOOGLE",
            availableSecondary = ""
        )

        every { envReader.getByKey(UserEnvKeys.GOOGLE_WEB_CLIENT_ID) } returns "google-client-id"

        // UniOne - present
        every { envReader.getByKey(UserEnvKeys.UNIONE_API_KEY_FILE) } returns "unione.key"
        every { envReader.readSecret("unione.key") } returns "u-key"
        every { envReader.getByKey(UserEnvKeys.UNIONE_URL) } returns "https://unione"
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_EMAIL) } returns "noreply@example.com"
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_NAME) } returns "Example"
        every { envReader.getByKey(UserEnvKeys.UNIONE_TRACK_DOMAIN) } returns "track.example.com"
        every { envReader.getByKey(UserEnvKeys.UNIONE_API_SEND) } returns "/send"

        // Resend - present
        every { envReader.getByKey(UserEnvKeys.RESEND_API_KEY_FILE) } returns "resend.key"
        every { envReader.readSecret("resend.key") } returns "r-key"
        every { envReader.getByKey(UserEnvKeys.RESEND_URL) } returns "https://resend"
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_EMAIL) } returns "noreply@example.com"
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_NAME) } returns "Example"

        val config = factory.create()

        val uniOne = config.uniOneConfig
        assertNotNull(uniOne)
        assertEquals(
            UniOneConfig(
                apiKey = "u-key",
                url = "https://unione",
                fromEmail = "noreply@example.com",
                fromName = "Example",
                trackDomain = "track.example.com",
                apiSend = "/send"
            ),
            uniOne
        )

        val resend = config.resendConfig
        assertNotNull(resend)
        assertEquals(
            ResendConfig(
                apiKey = "r-key",
                url = "https://resend",
                fromEmail = "noreply@example.com",
                fromName = "Example"
            ),
            resend
        )
    }

    private fun stubBaseEnv(availablePrimary: String, availableSecondary: String) {
        every { envReader.getByKey(UserEnvKeys.JWT_SECRET_FILE) } returns "jwt.secret"
        every { envReader.getByKey(UserEnvKeys.ADMIN_ACCOUNTS_JSON_SECRET_FILE) } returns "admins.json"

        every { envReader.getByKey(UserEnvKeys.ACCESS_TOKEN_VALIDITY_HOURS) } returns "12"
        every { envReader.getByKey(UserEnvKeys.REFRESH_TOKEN_VALIDITY_DAYS) } returns "30"
        every { envReader.getByKey(UserEnvKeys.AUTH_REALM) } returns "realm"

        every { envReader.readSecret("jwt.secret") } returns "jwt-secret"
        every { envReader.readSecret("admins.json") } returns """{"admins":[{"email":"admin@example.com","password":"pass"}]}"""

        every { envReader.getByKeyOrNull(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_PRIMARY) } returns availablePrimary
        every { envReader.getByKeyOrNull(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_SECONDARY) } returns availableSecondary
    }
}

