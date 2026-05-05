package io.github.mudrichenkoevgeny.backend.core.security.aescryptor

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString

/**
 * Provides symmetric encryption and decryption services for sensitive data.
 * * Primarily used to secure secrets before storage in persistent databases
 * to ensure that compromised data remains unreadable without the master key.
 */
interface AesCryptor {

    /**
     * Encrypts the given plain text using AES-256-GCM.
     *
     * @param decryptedString The raw string to be encrypted.
     * @return [AppResult.Success] containing the [EncryptedString].
     */
    fun encrypt(decryptedString: DecryptedString): AppResult<EncryptedString>

    /**
     * Decrypts the given cipher text back to its original plain text.
     *
     * @param encryptedString The [EncryptedString] produced by [encrypt].
     * @return [AppResult.Success] containing the [DecryptedString].
     */
    fun decrypt(encryptedString: EncryptedString): AppResult<DecryptedString>
}