package io.github.mudrichenkoevgeny.backend.feature.user.model.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import java.time.Instant

data class User(
    val id: UserId,
    val role: UserRole,
    val accountStatus: UserAccountStatus,
    val lastLoginAt: Instant?,
    val lastActiveAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant?
)