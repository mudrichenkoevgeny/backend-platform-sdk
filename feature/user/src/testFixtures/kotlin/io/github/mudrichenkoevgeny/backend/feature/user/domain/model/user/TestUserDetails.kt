package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserSummary
import kotlin.time.Clock
import kotlin.time.Instant

fun createTestUserInternal(
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
) = UserInternal(
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

fun createTestUserPrivate(
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
) = UserPrivate(
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

fun createTestUserSummary(
    id: UserId = UserId.generate(),
    role: UserRole = UserRole.ADMIN,
    accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
    authorityLevel: Int = 10,
    isTotpEnabled: Boolean = false,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
): UserSummary = createTestUserInternal(
    id = id,
    role = role,
    accountStatus = accountStatus,
    authorityLevel = authorityLevel,
    isTotpEnabled = isTotpEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
).toUserSummary()
