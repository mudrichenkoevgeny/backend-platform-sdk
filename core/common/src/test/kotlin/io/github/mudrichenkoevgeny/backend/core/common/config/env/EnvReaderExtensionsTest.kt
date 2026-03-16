package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnvReaderExtensionsTest {

    private val reader = mockk<EnvReader>()

    @Test
    fun `getStringList splits and trims values`() {
        every { reader.getByKeyOrNull("KEY") } returns " a , b ,  ,c "

        val result = reader.getStringList("KEY")

        assertEquals(listOf("a", "b", "c"), result)
    }

    @Test
    fun `getStringList returns empty list when missing`() {
        every { reader.getByKeyOrNull("MISSING") } returns null

        val result = reader.getStringList("MISSING")

        assertTrue(result.isEmpty())
    }

    @Serializable
    data class SecretPayload(val value: String)

    @Test
    fun `readJsonSecret decodes json secret`() {
        every { reader.readSecret("secret.json") } returns """{"value":"ok"}"""

        val payload: SecretPayload = reader.readJsonSecret("secret.json", FoundationJson)

        assertEquals(SecretPayload("ok"), payload)
    }
}

