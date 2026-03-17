package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailParserConfigHolderTest {

    @BeforeEach
    fun setUp() {
        EmailParserConfigHolder.resetForTests()
    }

    @Test
    fun `get returns default config when not set`() {
        val config = EmailParserConfigHolder.get()

        assertEquals(EmailParserConfig(), config)
    }

    @Test
    fun `get returns config previously set`() {
        val custom = EmailParserConfig(
            resourceFileName = "custom",
            resourceFileExtension = "json",
            resourcePaths = listOf("x"),
            supportedLocales = setOf("en")
        )

        EmailParserConfigHolder.set(custom)

        assertEquals(custom, EmailParserConfigHolder.get())
    }
}

