package io.github.mudrichenkoevgeny.backend.feature.user.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.feature.user.config.envkeys.UserEnvKeys
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserConfigFactoryImplTest {

    private companion object {
        private const val JWT_SECRET = "jwt-secret"
        private const val ADMIN_JSON = """{"admins":[{"email":"admin@example.com","password":"pass"}]}"""
        private const val GOOGLE_ID = "google-client-id"
        private const val AUTH_PRIMARY = "GOOGLE"
        private const val AUTH_SECONDARY = "EMAIL"
        private const val U_KEY = "u-key"
        private const val U_URL = "https://unione"
        private const val R_KEY = "r-key"
        private const val R_URL = "https://resend"
    }

    private val envReader = mockk<EnvReader>()
    private val factory = UserConfigFactoryImpl(envReader)

    @Test
    fun `create builds config and leaves email providers null when missing`() {
        stubBaseEnv(
            primary = AUTH_PRIMARY,
            secondary = AUTH_SECONDARY
        )

        every { envReader.getByKey(UserEnvKeys.GOOGLE_WEB_CLIENT_ID) } returns GOOGLE_ID

        every { envReader.getByKey(UserEnvKeys.UNIONE_API_KEY_FILE) } returns "unione.key"
        every { envReader.readSecret("unione.key") } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_URL) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_EMAIL) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_NAME) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_TRACK_DOMAIN) } returns ""
        every { envReader.getByKey(UserEnvKeys.UNIONE_API_SEND) } returns ""

        every { envReader.getByKey(UserEnvKeys.RESEND_API_KEY_FILE) } returns "resend.key"
        every { envReader.readSecret("resend.key") } returns ""
        every { envReader.getByKey(UserEnvKeys.RESEND_URL) } returns ""
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_EMAIL) } returns ""
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_NAME) } returns ""

        val config = factory.create()

        assertEquals(JWT_SECRET, config.jwtSecret)
        assertEquals(1, config.adminAccountsList.size)
        assertEquals("admin@example.com", config.adminAccountsList.first().email)
        assertEquals(GOOGLE_ID, config.googleWebClientId)
        assertNull(config.uniOneConfig)
        assertNull(config.resendConfig)

        val auth = config.managementAuthSettings
        assertEquals(listOf(UserAuthProvider.GOOGLE), auth.availableAuthProviders.primary)
        assertEquals(listOf(UserAuthProvider.EMAIL), auth.availableAuthProviders.secondary)
    }

    @Test
    fun `create builds UniOne and Resend configs when all values present`() {
        stubBaseEnv(primary = AUTH_PRIMARY, secondary = "")

        every { envReader.getByKey(UserEnvKeys.GOOGLE_WEB_CLIENT_ID) } returns GOOGLE_ID

        every { envReader.getByKey(UserEnvKeys.UNIONE_API_KEY_FILE) } returns "unione.key"
        every { envReader.readSecret("unione.key") } returns U_KEY
        every { envReader.getByKey(UserEnvKeys.UNIONE_URL) } returns U_URL
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_EMAIL) } returns "noreply@example.com"
        every { envReader.getByKey(UserEnvKeys.UNIONE_FROM_NAME) } returns "Example"
        every { envReader.getByKey(UserEnvKeys.UNIONE_TRACK_DOMAIN) } returns "track.example.com"
        every { envReader.getByKey(UserEnvKeys.UNIONE_API_SEND) } returns "/send"

        every { envReader.getByKey(UserEnvKeys.RESEND_API_KEY_FILE) } returns "resend.key"
        every { envReader.readSecret("resend.key") } returns R_KEY
        every { envReader.getByKey(UserEnvKeys.RESEND_URL) } returns R_URL
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_EMAIL) } returns "noreply@example.com"
        every { envReader.getByKey(UserEnvKeys.RESEND_FROM_NAME) } returns "Example"

        val config = factory.create()

        assertNotNull(config.uniOneConfig)
        assertEquals(U_KEY, config.uniOneConfig?.apiKey)
        assertEquals(U_URL, config.uniOneConfig?.url)

        assertNotNull(config.resendConfig)
        assertEquals(R_KEY, config.resendConfig?.apiKey)
        assertEquals(R_URL, config.resendConfig?.url)
    }

    private fun stubBaseEnv(primary: String, secondary: String) {
        every { envReader.getByKey(UserEnvKeys.JWT_SECRET_FILE) } returns "jwt.secret"
        every { envReader.readSecret("jwt.secret") } returns JWT_SECRET

        every { envReader.getByKey(UserEnvKeys.ADMIN_ACCOUNTS_JSON_SECRET_FILE) } returns "admins.json"
        every { envReader.readSecret("admins.json") } returns ADMIN_JSON

        every { envReader.getByKeyOrNull(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_PRIMARY) } returns primary
        every { envReader.getByKeyOrNull(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_SECONDARY) } returns secondary

        every { envReader.getByKey(UserEnvKeys.MAX_TOTAL_IDENTIFIERS) } returns "10"
        every { envReader.getByKey(UserEnvKeys.MAX_EMAIL_IDENTIFIERS) } returns "1"
        every { envReader.getByKey(UserEnvKeys.MAX_PHONE_IDENTIFIERS) } returns "1"
        every { envReader.getByKey(UserEnvKeys.MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER) } returns "1"
        every { envReader.getByKey(UserEnvKeys.MAX_ACTIVE_SESSIONS) } returns "5"
        every { envReader.getByKey(UserEnvKeys.ACCESS_TOKEN_EXPIRATION_SECONDS) } returns "3600"
        every { envReader.getByKey(UserEnvKeys.REFRESH_TOKEN_EXPIRATION_SECONDS) } returns "86400"
        every { envReader.getByKey(UserEnvKeys.ACCOUNT_DELETION_DELAY_SECONDS) } returns "3600"
    }
}