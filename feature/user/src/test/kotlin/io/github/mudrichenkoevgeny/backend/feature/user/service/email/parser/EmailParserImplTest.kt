package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailParserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EmailParserImplTest {

    private val parser = EmailParserImpl(
        config = EmailParserConfig(
            resourceFileName = FILE_NAME,
            resourceFileExtension = FILE_EXTENSION,
            resourcePaths = listOf(PATH),
            supportedLocales = setOf(LOCALE_EN)
        )
    )

    @Test
    fun `getTemplate loads template from resources and replaces args`() {
        val template = parser.getTemplate(
            key = KEY_VERIFICATION,
            args = mapOf(ARG_CODE to "123456", ARG_IP to "127.0.0.1"),
            locale = "en-US"
        )

        assertNotNull(template)
        template as EmailTemplate

        assertEquals("Your code: 123456", template.subject)
        assertEquals("<p>Code: 123456</p><p>IP: 127.0.0.1</p>", template.body)
    }

    @Test
    fun `getTemplate falls back to default locale when locale missing`() {
        val template = parser.getTemplate(
            key = KEY_LOGIN,
            args = mapOf(ARG_DEVICE to "Pixel", ARG_IP to "10.0.0.1"),
            locale = "ru"
        )

        assertNotNull(template)
        template as EmailTemplate

        assertEquals("Login from Pixel", template.subject)
        assertEquals("IP 10.0.0.1", template.body)
    }

    private companion object {
        const val PATH = "localization"
        const val LOCALE_EN = "en"

        const val FILE_NAME = "email_messages"
        const val FILE_EXTENSION = "json"

        const val KEY_VERIFICATION = "verification_code"
        const val KEY_LOGIN = "successful_login"

        const val ARG_CODE = "code"
        const val ARG_IP = "ipAddress"
        const val ARG_DEVICE = "deviceName"
    }
}

