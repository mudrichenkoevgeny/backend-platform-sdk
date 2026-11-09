package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user

import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserStorageConfiguration
import kotlin.time.Instant

/**
 * Domain-layer parameter holder for creating a user entity profile in [UserManager].
 *
 * Encapsulates core user account settings, role, authority level, permissions, and profile fields,
 * excluding authentication credentials (email/password).
 *
 * @property role assigned role for the new user account
 * @property accountStatus initial account status
 * @property authorityLevel numerical rank of the user's authority
 * @property permissions set of explicit permission codes assigned to the new user
 * @property storageConfiguration optional storage configuration overrides and limits
 * @property nickname optional handle nickname
 * @property displayName optional profile display name
 * @property avatarFileId optional avatar image file identifier
 * @property birthdate optional user birthdate timestamp
 */
public data class CreateUserAccountParams(
    val role: UserRole = UserRole.USER,
    val accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
    val authorityLevel: Int = 0,
    val permissions: Set<PermissionCode> = emptySet(),
    val storageConfiguration: UserStorageConfiguration? = null,
    val nickname: String? = null,
    val displayName: String? = null,
    val avatarFileId: FileId? = null,
    val birthdate: Instant? = null
)
