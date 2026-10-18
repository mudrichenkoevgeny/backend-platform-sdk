package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmailParserImplTest {

    private val parser = EmailParserImpl()

    @Test
    fun `getTemplate resolves embedded template and formats args`() {
        val template = parser.getTemplate(
            key = KEY_VERIFICATION,
            args = mapOf(ARG_CODE to "123456"),
            locale = "en-US"
        )

        assertNotNull(template)
        template as EmailTemplate

        assertEquals("Verification Code", template.subject)
        assertTrue(template.body.contains("Your code: 123456"))
    }

    @Test
    fun `getTemplate resolves Russian localized template when requested`() {
        val template = parser.getTemplate(
            key = KEY_LOGIN,
            args = mapOf(ARG_DEVICE to "Pixel", ARG_IP to "10.0.0.1"),
            locale = "ru"
        )

        assertNotNull(template)
        template as EmailTemplate

        assertEquals("Успешный вход", template.subject)
        assertTrue(template.body.contains("IP: 10.0.0.1"))
        assertTrue(template.body.contains("Pixel"))
    }

    private companion object {
        const val KEY_VERIFICATION = "verification_code"
        const val KEY_LOGIN = "successful_login"

        const val ARG_CODE = "code"
        const val ARG_IP = "ipAddress"
        const val ARG_DEVICE = "deviceName"
    }
}