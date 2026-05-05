package io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.aescryptor.AesCryptor
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.security.util.Base32
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TotpCryptoProcessorImplTest {

    private val aesCryptor = mockk<AesCryptor>()
    private val securityConfig = mockk<SecurityConfig>()
    private lateinit var processor: TotpCryptoProcessorImpl

    private val testAuthRealm = "TestRealm"
    private val testAccount = "test@example.com"
    private val testSecretBase32 = "JBSWY3DPEBLW64TMMQQQ===="
    private val decryptedSecret = DecryptedString(testSecretBase32)
    private val encryptedSecret = EncryptedString("encrypted-bytes")

    @BeforeEach
    fun setup() {
        every { securityConfig.authRealm } returns testAuthRealm
        processor = TotpCryptoProcessorImpl(aesCryptor, securityConfig)
    }

    @Test
    fun `generateNewSecret creates valid secret and otpauth url`() {
        every { aesCryptor.encrypt(any()) } returns AppResult.Success(encryptedSecret)

        val result = processor.generateNewSecret(testAccount)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data

        assertEquals(32, data.decryptedSecret.value.length)
        assertEquals(encryptedSecret, data.encryptedSecret)
        assertTrue(data.otpAuthUrl.contains("otpauth://totp/$testAuthRealm:$testAccount"))
        assertTrue(data.otpAuthUrl.contains("secret=${data.decryptedSecret.value}"))
    }

    @Test
    fun `isCodeValid returns true for correct current code`() {
        every { aesCryptor.decrypt(encryptedSecret) } returns AppResult.Success(decryptedSecret)

        val secretBytes = Base32.decode(testSecretBase32)
        val counter = System.currentTimeMillis() / 1000 / 30

        val validCode = invokeGenerateTotp(secretBytes, counter)

        val result = processor.isCodeValid(validCode, encryptedSecret)

        assertTrue(result is AppResult.Success)
        assertEquals(true, (result as AppResult.Success).data)
    }

    @Test
    fun `isCodeValid returns true for code within window (minus 1 step)`() {
        every { aesCryptor.decrypt(encryptedSecret) } returns AppResult.Success(decryptedSecret)

        val secretBytes = Base32.decode(testSecretBase32)
        val counter = (System.currentTimeMillis() / 1000 / 30) - 1
        val pastCode = invokeGenerateTotp(secretBytes, counter)

        val result = processor.isCodeValid(pastCode, encryptedSecret)

        assertTrue(result is AppResult.Success)
        assertEquals(true, (result as AppResult.Success).data)
    }

    @Test
    fun `isCodeValid returns false for completely wrong code`() {
        every { aesCryptor.decrypt(encryptedSecret) } returns AppResult.Success(decryptedSecret)

        val result = processor.isCodeValid("000000", encryptedSecret)

        assertTrue(result is AppResult.Success)
        assertEquals(false, (result as AppResult.Success).data)
    }

    @Test
    fun `getOtpAuthUrl returns correctly formatted string`() {
        every { aesCryptor.decrypt(encryptedSecret) } returns AppResult.Success(decryptedSecret)

        val result = processor.getOtpAuthUrl(testAccount, encryptedSecret)

        assertTrue(result is AppResult.Success)
        val url = (result as AppResult.Success).data
        assertEquals(
            "otpauth://totp/$testAuthRealm:$testAccount?secret=$testSecretBase32&issuer=$testAuthRealm&algorithm=SHA1&digits=6&period=30",
            url
        )
    }

    @Test
    fun `generateRecoveryCodes returns requested number of formatted codes`() {
        val count = 5
        val result = processor.generateRecoveryCodes(count)

        assertTrue(result is AppResult.Success)
        val codes = (result as AppResult.Success).data
        assertEquals(count, codes.size)

        val firstCode = codes.first().value
        assertEquals(14, firstCode.length)
        assertTrue(firstCode.matches(Regex("^[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$")))
    }

    /**
     * Helper to replicate the private generateTotp logic for testing purposes
     */
    private fun invokeGenerateTotp(secret: ByteArray, interval: Long): String {
        val method = processor.javaClass.getDeclaredMethod("generateTotp", ByteArray::class.java, Long::class.java)
        method.isAccessible = true
        return method.invoke(processor, secret, interval) as String
    }
}