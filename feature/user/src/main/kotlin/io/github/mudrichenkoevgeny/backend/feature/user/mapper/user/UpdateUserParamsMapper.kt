package io.github.mudrichenkoevgeny.backend.feature.user.mapper.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.toUpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.validateAndMap
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.toFileIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.user.ManagementUpdateUserRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.user.SelfUpdateUserRequest
import kotlin.time.Instant

/**
 * Converts a [ManagementUpdateUserRequest] DTO into a domain-layer [UpdateUserParams] holder.
 *
 * Performs strict validation and parsing for present non-null patch values (e.g. enum wire strings,
 * [PermissionCode] instances, and [FileId] UUID strings).
 *
 * @receiver [ManagementUpdateUserRequest] HTTP request body payload containing 3-state patch fields.
 * @return [UpdateUserParams] domain model containing mapped [UpdateField] values.
 * @throws [RequestHandlingException] if any present non-null string field cannot be parsed into its domain representation.
 */
fun ManagementUpdateUserRequest.toUpdateUserParams(): UpdateUserParams {
    return UpdateUserParams(
        accountStatus = accountStatus.validateAndMap(UserApiFields.ACCOUNT_STATUS) { accountStatus ->
            UserAccountStatus.fromValueOrNull(accountStatus)
        },
        authorityLevel = authorityLevel.toUpdateField(),
        permissions = permissionCodes.validateAndMap(UserApiFields.PERMISSION_CODES) { set ->
            set.mapToSet { PermissionCode(it) }
        },
        lockoutType = lockoutType.validateAndMap(UserApiFields.ACCOUNT_LOCKOUT_TYPE) { lockoutType ->
            AccountLockoutType.fromValueOrNull(lockoutType)
        },
        temporaryLockoutUntil = temporaryLockoutUntil
            .validateAndMap(UserApiFields.TEMPORARY_LOCKOUT_UNTIL) { temporaryLockoutUntil ->
                Instant.fromEpochMilliseconds(temporaryLockoutUntil)
            },
        storageConfiguration = storageConfiguration.toUpdateField(),
        nickname = nickname.toUpdateField(),
        displayName = displayName.toUpdateField(),
        avatarFileId = avatarFileId.validateAndMap(UserApiFields.AVATAR_FILE_ID) { avatarFileId ->
            avatarFileId.toFileIdOrNull()
        },
        birthdate = birthdate.validateAndMap(UserApiFields.BIRTHDATE) { birthdate ->
            Instant.fromEpochMilliseconds(birthdate)
        }
    )
}

/**
 * Converts a [SelfUpdateUserRequest] DTO into a domain-layer [UpdateUserParams] holder.
 *
 * Performs strict validation and parsing for present non-null patch values (e.g. [FileId] UUID strings).
 *
 * @receiver [SelfUpdateUserRequest] HTTP request body payload containing 3-state patch fields.
 * @return [UpdateUserParams] domain model containing mapped [UpdateField] values.
 * @throws [RequestHandlingException] if any present non-null string field cannot be parsed into its domain representation.
 */
fun SelfUpdateUserRequest.toUpdateUserParams(): UpdateUserParams {
    return UpdateUserParams(
        nickname = nickname.toUpdateField(),
        displayName = displayName.toUpdateField(),
        avatarFileId = avatarFileId.validateAndMap(UserApiFields.AVATAR_FILE_ID) { avatarFileId ->
            avatarFileId.toFileIdOrNull()
        },
        birthdate = birthdate.validateAndMap(UserApiFields.BIRTHDATE) { birthdate ->
            Instant.fromEpochMilliseconds(birthdate)
        }
    )
}
