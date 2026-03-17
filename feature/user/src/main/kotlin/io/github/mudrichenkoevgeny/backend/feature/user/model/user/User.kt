package io.github.mudrichenkoevgeny.backend.feature.user.model.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import java.time.Instant

/**
 * Current state of a user account.
 *
 * Used by managers and routes to expose user metadata while keeping ids strongly typed.
 *
 * @property id Stable identifier of the user.
 * @property role Role assigned to the user.
 * @property accountStatus Current account status (active/blocked/etc.).
 * @property lastLoginAt Last successful login time, if known.
 * @property lastActiveAt Last observed activity time, if known.
 * @property createdAt User creation time.
 * @property updatedAt Last update time, if known.
 */
data class User(
    val id: UserId,
    val role: UserRole,
    val accountStatus: UserAccountStatus,
    val lastLoginAt: Instant?,
    val lastActiveAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant?
)