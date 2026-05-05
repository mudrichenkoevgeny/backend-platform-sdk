package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usertotpsettings

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.totp.UserTotpSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Persistence API for TOTP (Time-based One-Time Password) security settings.
 *
 * Manages the lifecycle of sensitive security data including encrypted secrets,
 * encrypted recovery codes, and verification state in the persistent storage.
 */
interface UserTotpSettingsRepository {

    /**
     * Creates or updates unconfirmed TOTP settings for a user.
     * This is the entry point for the 2FA setup process where the secret is generated
     * but not yet verified by a successful code entry.
     *
     * @param userId The unique identifier of the user.
     * @param encryptedSecret The TOTP secret key, encrypted for secure storage.
     * @return [AppResult.Success] containing the initiated [UserTotpSettings].
     */
    suspend fun upsertUnconfirmedSettings(
        userId: UserId,
        encryptedSecret: EncryptedString
    ): AppResult<UserTotpSettings>

    /**
     * Finalizes TOTP activation for a user.
     * Sets the confirmation flag to true and stores the initial set of recovery codes.
     *
     * @param userId The unique identifier of the user.
     * @param encryptedRecoveryCodes A list of generated recovery codes, encrypted for storage.
     * @return [AppResult.Success] containing the confirmed [UserTotpSettings].
     */
    suspend fun confirmTotp(
        userId: UserId,
        encryptedRecoveryCodes: List<EncryptedString>
    ): AppResult<UserTotpSettings>

    /**
     * Updates specific fields of the user's TOTP settings.
     *
     * @param userId The unique identifier of the user.
     * @param encryptedSecret An update for the encrypted secret key.
     * @param isConfirmed An update for the activation status.
     * @param encryptedRecoveryCodes An update for the list of encrypted recovery codes.
     * @param lastUsedAt An update for the timestamp of the last successful OTP verification.
     * @return [AppResult.Success] with the updated [UserTotpSettings].
     */
    suspend fun updateSettings(
        userId: UserId,
        encryptedSecret: UpdateField<EncryptedString> = UpdateField.Ignore,
        isConfirmed: UpdateField<Boolean> = UpdateField.Ignore,
        encryptedRecoveryCodes: UpdateField<List<EncryptedString>?> = UpdateField.Ignore,
        lastUsedAt: UpdateField<Instant?> = UpdateField.Ignore
    ): AppResult<UserTotpSettings>

    /**
     * Retrieves the TOTP settings associated with the given user.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with [UserTotpSettings] if found, null otherwise.
     */
    suspend fun getSettingsByUserId(userId: UserId): AppResult<UserTotpSettings?>

    /**
     * Removes all TOTP-related settings for the specified user.
     * Typically called when a user disables two-factor authentication.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with [Unit] on successful deletion.
     */
    suspend fun deleteSettings(userId: UserId): AppResult<Unit>
}