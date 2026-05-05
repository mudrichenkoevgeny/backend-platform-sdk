package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Clock

fun createTestUserDetails(
    id: UserId = UserId.generate(),
    role: UserRole = UserRole.ADMIN,
    accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
    permissionCodes: Set<PermissionCode> = emptySet(),
    authorityLevel: Int = 10
) = UserDetails(
    id = id,
    role = role,
    accountStatus = accountStatus,
    accountStatusBeforeDeletion = null,
    authorityLevel = authorityLevel,
    permissionCodes = permissionCodes,
    isTotpEnabled = false,
    createdAt = Clock.System.now(),
    updatedAt = null
)