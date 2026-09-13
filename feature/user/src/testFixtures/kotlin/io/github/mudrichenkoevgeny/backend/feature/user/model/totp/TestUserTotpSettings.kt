package io.github.mudrichenkoevgeny.backend.feature.user.model.totp

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

fun createTestUserTotpSettings(
    userId: UserId = UserId.generate(),
    encryptedSecret: EncryptedString = EncryptedString("encrypted_secret"),
    isConfirmed: Boolean = true,
    encryptedRecoveryCodes: List<EncryptedString>? = null,
    lastUsedAt: Instant? = null
) = UserTotpSettings(
    userId = userId,
    encryptedSecret = encryptedSecret,
    isConfirmed = isConfirmed,
    encryptedRecoveryCodes = encryptedRecoveryCodes,
    lastUsedAt = lastUsedAt
)
