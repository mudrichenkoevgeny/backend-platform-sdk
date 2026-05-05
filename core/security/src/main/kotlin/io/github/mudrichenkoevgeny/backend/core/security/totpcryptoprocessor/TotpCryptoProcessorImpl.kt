package io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.security.aescryptor.AesCryptor
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.model.GeneratedTotpSecret
import io.github.mudrichenkoevgeny.backend.core.security.util.Base32
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Default [TotpCryptoProcessor] implementation for managing MFA lifecycles.
 *
 * Handles RFC 6238 compliant TOTP generation and verification using HMAC-SHA1.
 * Integrates with [AesCryptor] to ensure secrets are always encrypted at rest.
 * Supports a ±1 time-step validation window and recovery code generation.
 */
@Singleton
class TotpCryptoProcessorImpl @Inject constructor(
    private val aesCryptor: AesCryptor,
    private val securityConfig: SecurityConfig
) : TotpCryptoProcessor {

    private val secureRandom = SecureRandom()
    private val issuer: String get() = securityConfig.authRealm

    override fun generateNewSecret(accountName: String): AppResult<GeneratedTotpSecret> {
        return try {
            val bytes = ByteArray(SECRET_SIZE_BYTES)
            secureRandom.nextBytes(bytes)
            val decryptedSecret = DecryptedString(Base32.encode(bytes))
            val otpAuthUrl = buildOtpAuthUrl(accountName, decryptedSecret)

            aesCryptor.encrypt(decryptedSecret).mapSuccess { encryptedSecret ->
                GeneratedTotpSecret(
                    decryptedSecret = decryptedSecret,
                    encryptedSecret = encryptedSecret,
                    otpAuthUrl = otpAuthUrl
                )
            }
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override fun isCodeValid(code: String, encryptedSecret: EncryptedString): AppResult<Boolean> {
        return try {
            aesCryptor.decrypt(encryptedSecret).mapSuccess { decryptedSecret ->
                val secretBytes = Base32.decode(decryptedSecret.value)
                val counter = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS

                VALIDATION_WINDOW_RANGE.any { window ->
                    generateTotp(secretBytes, counter + window) == code
                }
            }
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override fun getOtpAuthUrl(accountName: String, encryptedSecret: EncryptedString): AppResult<String> {
        return try {
            aesCryptor.decrypt(encryptedSecret).mapSuccess { decryptedSecret ->
                buildOtpAuthUrl(accountName, decryptedSecret)
            }
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override fun generateRecoveryCodes(count: Int): AppResult<List<DecryptedString>> {
        return try {
            val codes = List(count) {
                val rawCode = (1..RECOVERY_CODE_LENGTH).map {
                    RECOVERY_CODE_ALPHABET[secureRandom.nextInt(RECOVERY_CODE_ALPHABET.length)]
                }.joinToString("")
                    .chunked(RECOVERY_CODE_CHUNK_SIZE)
                    .joinToString("-")

                DecryptedString(rawCode)
            }
            AppResult.Success(codes)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    private fun generateTotp(secret: ByteArray, interval: Long): String {
        val data = ByteBuffer.allocate(8).putLong(interval).array()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret, "RAW"))
        val hash = mac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0xf
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)

        val otp = binary % 10.0.pow(OTP_DIGITS.toDouble()).toInt()
        return otp.toString().padStart(OTP_DIGITS, '0')
    }

    private fun buildOtpAuthUrl(accountName: String, decryptedSecret: DecryptedString): String {
        return "otpauth://totp/$issuer:$accountName?secret=${decryptedSecret}&issuer=$issuer&algorithm=$URL_ALGORITHM&digits=$OTP_DIGITS&period=$TIME_STEP_SECONDS"
    }

    companion object {
        private const val SECRET_SIZE_BYTES = 20
        private const val TIME_STEP_SECONDS = 30
        private const val OTP_DIGITS = 6
        private val VALIDATION_WINDOW_RANGE = -1..1

        private const val HMAC_ALGORITHM = "HmacSHA1"
        private const val URL_ALGORITHM = "SHA1"

        private const val RECOVERY_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val RECOVERY_CODE_LENGTH = 12
        private const val RECOVERY_CODE_CHUNK_SIZE = 4
    }
}