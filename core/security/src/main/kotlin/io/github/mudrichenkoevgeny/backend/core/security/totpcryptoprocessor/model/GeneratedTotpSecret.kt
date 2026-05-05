package io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.model

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString

/**
 * Result of a new TOTP factor generation.
 *
 * @property decryptedSecret The raw Base32 encoded secret to be shared with the user (via QR or manual entry).
 * @property encryptedSecret The secret encrypted for secure persistence in the database.
 * @property otpAuthUrl The standard `otpauth://` URI for QR code generation.
 */
data class GeneratedTotpSecret(
    val decryptedSecret: DecryptedString,
    val encryptedSecret: EncryptedString,
    val otpAuthUrl: String
)