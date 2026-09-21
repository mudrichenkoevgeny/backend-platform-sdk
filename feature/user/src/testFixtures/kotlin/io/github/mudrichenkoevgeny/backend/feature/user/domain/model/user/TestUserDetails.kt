package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Clock
import kotlin.time.Instant

fun createTestUserDetails(
    id: UserId = UserId.generate(),
    role: UserRole = UserRole.ADMIN,
    accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
    accountStatusOnRestore: UserAccountStatus? = null,
    authorityLevel: Int = 10,
    permissionCodes: Set<PermissionCode> = emptySet(),
    isTotpEnabled: Boolean = false,
    lastLoginAt: Instant? = null,
    lastActiveAt: Instant? = null,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null,
    scheduledPermanentDeletionAt: Instant? = null,
    lockoutType: AccountLockoutType = AccountLockoutType.NONE,
    temporaryLockoutUntil: Instant? = null
) = UserDetails(
    id = id,
    role = role,
    accountStatus = accountStatus,
    accountStatusOnRestore = accountStatusOnRestore,
    authorityLevel = authorityLevel,
    permissionCodes = permissionCodes,
    isTotpEnabled = isTotpEnabled,
    lastLoginAt = lastLoginAt,
    lastActiveAt = lastActiveAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    scheduledPermanentDeletionAt = scheduledPermanentDeletionAt,
    lockoutType = lockoutType,
    temporaryLockoutUntil = temporaryLockoutUntil
)
