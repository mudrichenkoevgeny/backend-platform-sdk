package io.github.mudrichenkoevgeny.backend.core.security.aescryptor

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class AesCryptorImplTest {

    private val validSecret = Base64.getEncoder().encodeToString(ByteArray(32) { 1.toByte() })
    private val securityConfig = mockk<SecurityConfig> {
        every { totpEncryptionSecret } returns validSecret
    }

    private val cryptor = AesCryptorImpl(securityConfig)

    @Test
    fun `init throws exception when secret key is too short`() {
        val shortSecret = Base64.getEncoder().encodeToString(ByteArray(16))
        val config = mockk<SecurityConfig> {
            every { totpEncryptionSecret } returns shortSecret
        }

        assertThrows<IllegalArgumentException> {
            AesCryptorImpl(config)
        }
    }

    @Test
    fun `encrypt and decrypt returns original string`() {
        val originalText = "top-secret-payload"
        val decryptedInput = DecryptedString(originalText)

        val encryptResult = cryptor.encrypt(decryptedInput)
        assertTrue(encryptResult is AppResult.Success)
        val encryptedString = (encryptResult as AppResult.Success).data

        val decryptResult = cryptor.decrypt(encryptedString)
        assertTrue(decryptResult is AppResult.Success)
        val decryptedOutput = (decryptResult as AppResult.Success).data

        assertEquals(originalText, decryptedOutput.value)
    }

    @Test
    fun `encrypt produces different ciphertexts for same input due to random IV`() {
        val input = DecryptedString("same-text")

        val result1 = (cryptor.encrypt(input) as AppResult.Success).data
        val result2 = (cryptor.encrypt(input) as AppResult.Success).data

        assertNotEquals(result1.value, result2.value)
    }

    @Test
    fun `decrypt returns error for invalid base64`() {
        val invalidEncrypted = EncryptedString("not-a-base64-string!")

        val result = cryptor.decrypt(invalidEncrypted)

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `decrypt returns error for data that is too short`() {
        val shortData = Base64.getEncoder().encodeToString(ByteArray(5))
        val invalidEncrypted = EncryptedString(shortData)

        val result = cryptor.decrypt(invalidEncrypted)

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `decrypt returns error when using different key`() {
        val input = DecryptedString("secret")
        val encrypted = (cryptor.encrypt(input) as AppResult.Success).data

        val otherSecret = Base64.getEncoder().encodeToString(ByteArray(32) { 2.toByte() })
        val otherConfig = mockk<SecurityConfig> {
            every { totpEncryptionSecret } returns otherSecret
        }
        val otherCryptor = AesCryptorImpl(otherConfig)

        val result = otherCryptor.decrypt(encrypted)

        assertTrue(result is AppResult.Error)
    }
}