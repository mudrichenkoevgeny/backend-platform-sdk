package io.github.mudrichenkoevgeny.backend.feature.user.manager.totp

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.totp.UserTotpSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Manager for handling TOTP business logic and persistence coordination.
 */
interface TotpManager {

    /**
     * Initializes TOTP setup by creating an unconfirmed settings record.
     *
     * @param userId The unique identifier of the user.
     * @param encryptedSecret The TOTP secret key, encrypted for secure storage.
     * @return [AppResult.Success] with the initial [UserTotpSettings].
     */
    suspend fun initiateTotpSetup(
        userId: UserId,
        encryptedSecret: EncryptedString
    ): AppResult<UserTotpSettings>

    /**
     * Confirms TOTP activation.
     * Encrypts the provided recovery codes, marks settings as confirmed, and enables TOTP for the user.
     *
     * @param userId The unique identifier of the user.
     * @param decryptedRecoveryCodes A list of plain-text recovery codes to be encrypted and stored.
     * @return [AppResult.Success] containing [Unit] on successful activation.
     */
    suspend fun confirmTotp(
        userId: UserId,
        decryptedRecoveryCodes: List<DecryptedString>
    ): AppResult<Unit>

    /**
     * Fully disables TOTP by removing settings and resetting the user's 'isTotpEnabled' flag.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] containing [Unit] if TOTP was successfully disabled.
     */
    suspend fun disableTotp(userId: UserId): AppResult<Unit>

    /**
     * Updates the last used timestamp for a TOTP token to prevent replay attacks.
     *
     * @param userId The unique identifier of the user.
     * @param timestamp The instant when the code was successfully verified.
     * @return [AppResult.Success] containing [Unit] if the timestamp was updated.
     */
    suspend fun markAsUsed(userId: UserId, timestamp: Instant): AppResult<Unit>

    /**
     * Verifies a 6-digit TOTP code against the user's secret.
     *
     * @param userId The unique identifier of the user.
     * @param code The 6-digit code provided by the user.
     * @return [AppResult.Success] if the code is valid, otherwise [AppResult.Error].
     */
    suspend fun verifyTotp(userId: UserId, code: String): AppResult<Unit>

    /**
     * Verifies a recovery code if the user has lost access to their TOTP device.
     *
     * @param userId The unique identifier of the user.
     * @param code The plain-text recovery code provided by the user.
     * @return [AppResult.Success] if the code is valid and has been consumed, otherwise [AppResult.Error].
     */
    suspend fun verifyTotpRecoveryCode(userId: UserId, code: String): AppResult<Unit>

    /**
     * Retrieves the current TOTP settings for the specified user.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with [UserTotpSettings] or null if not configured.
     */
    suspend fun getSettings(userId: UserId): AppResult<UserTotpSettings?>

    /**
     * Retrieves and decrypts all recovery codes associated with the user.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with a list of [DecryptedString] recovery codes.
     */
    suspend fun getDecryptedRecoveryCodes(userId: UserId): AppResult<List<DecryptedString>>

    /**
     * Replaces existing recovery codes with a new set.
     * Encrypts the new codes before storage and returns the decrypted versions to confirm persistence.
     *
     * @param userId The unique identifier of the user.
     * @param decryptedRecoveryCodes A new list of plain-text recovery codes to store.
     * @return [AppResult.Success] with the list of successfully stored [DecryptedString] codes.
     */
    suspend fun updateRecoveryCodes(
        userId: UserId,
        decryptedRecoveryCodes: List<DecryptedString>
    ): AppResult<List<DecryptedString>>
}