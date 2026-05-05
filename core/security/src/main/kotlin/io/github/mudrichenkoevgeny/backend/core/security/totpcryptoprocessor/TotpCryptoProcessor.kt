package io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.model.GeneratedTotpSecret
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString

/**
 * Processor for Time-based One-Time Password (TOTP) operations.
 *
 * Handles the lifecycle of TOTP secrets including generation, secure storage
 * via encryption, and verification of time-based tokens.
 */
interface TotpCryptoProcessor {

    /**
     * Generates a new random TOTP secret and prepares it for both the user and storage.
     *
     * @param accountName The identifier (e.g., email) to be displayed in the authenticator app.
     * @return [AppResult.Success] with [GeneratedTotpSecret].
     */
    fun generateNewSecret(accountName: String): AppResult<GeneratedTotpSecret>

    /**
     * Verifies a 6-digit TOTP code against a stored encrypted secret.
     *
     * @param code The 6-digit code provided by the user.
     * @param encryptedSecret The encrypted secret retrieved from the database.
     * @return [AppResult.Success] with `true` if the code is valid for the current time window.
     */
    fun isCodeValid(code: String, encryptedSecret: EncryptedString): AppResult<Boolean>

    /**
     * Reconstructs the `otpauth://` URI from a stored encrypted secret.
     *
     * @param accountName The identifier to be displayed in the authenticator app.
     * @param encryptedSecret The encrypted secret retrieved from the database.
     * @return [AppResult.Success] with the formatted URI string.
     */
    fun getOtpAuthUrl(accountName: String, encryptedSecret: EncryptedString): AppResult<String>

    /**
     * Generates a set of high-entropy recovery codes for emergency account access.
     *
     * @param count The number of codes to generate.
     * @return [AppResult.Success] with a list of [DecryptedString] containing recovery codes.
     */
    fun generateRecoveryCodes(count: Int = 10): AppResult<List<DecryptedString>>
}