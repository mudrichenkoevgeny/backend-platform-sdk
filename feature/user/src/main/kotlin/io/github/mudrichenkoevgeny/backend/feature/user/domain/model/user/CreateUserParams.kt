package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.user.UserStorageConfigurationPayload

/**
 * Domain-layer parameter holder for creating a new user account.
 *
 * Encapsulates all initial profile details, role, authority level, permissions, and storage configuration.
 *
 * @property email email address for the new user account
 * @property password optional initial plaintext password for the new user account
 * @property role assigned role for the new user account
 * @property accountStatus initial account status
 * @property authorityLevel numerical rank of the user's authority
 * @property permissions set of explicit permission codes assigned to the new user
 * @property storageConfiguration optional storage configuration overrides and limits
 * @property nickname optional handle nickname
 * @property displayName optional profile display name
 * @property avatarFileId optional avatar image file identifier
 */
data class CreateUserParams(
    val email: String,
    val password: String? = null,
    val role: UserRole,
    val accountStatus: UserAccountStatus,
    val authorityLevel: Int,
    val permissions: Set<PermissionCode>,
    val storageConfiguration: UserStorageConfigurationPayload? = null,
    val nickname: String? = null,
    val displayName: String? = null,
    val avatarFileId: FileId? = null
    // todo wait for shared update and add birthdate
)
