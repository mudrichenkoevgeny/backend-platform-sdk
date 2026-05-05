package io.github.mudrichenkoevgeny.backend.feature.user.model.totp

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Data model representing the Two-Factor Authentication (TOTP) configuration for a user.
 *
 * Holds sensitive security parameters including the [encryptedSecret] and [encryptedRecoveryCodes].
 * The [isConfirmed] flag indicates whether the user has successfully completed the setup
 * flow by verifying their first code.
 *
 * @property userId Unique identifier of the user owner.
 * @property encryptedSecret The TOTP shared secret, stored as an [EncryptedString].
 * @property isConfirmed Whether 2FA is fully activated and verified.
 * @property encryptedRecoveryCodes List of backup codes for account recovery, encrypted for storage.
 * @property lastUsedAt Timestamp of the last successful verification to prevent replay attacks and track activity.
 */
data class UserTotpSettings(
    val userId: UserId,
    val encryptedSecret: EncryptedString,
    val isConfirmed: Boolean,
    val encryptedRecoveryCodes: List<EncryptedString>?,
    val lastUsedAt: Instant?
)