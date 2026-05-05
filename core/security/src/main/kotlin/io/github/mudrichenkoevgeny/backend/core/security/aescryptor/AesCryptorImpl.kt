package io.github.mudrichenkoevgeny.backend.core.security.aescryptor

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [AesCryptor] implementation using AES-256-GCM.
 *
 * Provides symmetric encryption for sensitive data before storage. Uses a 12-byte IV
 * and 128-bit GCM tag for integrity. Resulting payload is a Base64 string containing
 * concatenated [IV + EncryptedData].
 */
@Singleton
class AesCryptorImpl @Inject constructor(
    securityConfig: SecurityConfig
) : AesCryptor {

    private val secretKey: SecretKeySpec
    private val secureRandom = SecureRandom()

    private val algorithm = "AES/GCM/NoPadding"
    private val tagLength = 128
    private val ivLength = 12
    private val minKeyLength = 32

    init {
        val keyBytes = Base64.getDecoder().decode(securityConfig.totpEncryptionSecret)
        require(keyBytes.size >= minKeyLength) { "Encryption secret must be at least 32 bytes." }
        secretKey = SecretKeySpec(keyBytes, "AES")
    }

    override fun encrypt(decryptedString: DecryptedString): AppResult<EncryptedString> {
        return try {
            val iv = ByteArray(ivLength)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(tagLength, iv))

            val encryptedBytes = cipher.doFinal(decryptedString.value.toByteArray(Charsets.UTF_8))

            val combined = iv + encryptedBytes
            AppResult.Success(EncryptedString(Base64.getEncoder().encodeToString(combined)))
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override fun decrypt(encryptedString: EncryptedString): AppResult<DecryptedString> {
        return try {
            val combined = Base64.getDecoder().decode(encryptedString.value)
            val minLength = ivLength + (tagLength / 8)

            if (combined.size < minLength) {
                throw IllegalArgumentException("Invalid encrypted data length")
            }

            val iv = combined.sliceArray(0 until ivLength)
            val encryptedBytes = combined.sliceArray(ivLength until combined.size)

            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(tagLength, iv))

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            AppResult.Success(DecryptedString(String(decryptedBytes, Charsets.UTF_8)))
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }
}