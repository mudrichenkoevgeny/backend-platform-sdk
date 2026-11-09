package io.github.mudrichenkoevgeny.backend.feature.user.mapper.user

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.CreateUserParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.toFileIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserStorageConfiguration
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.create.CreateByEmailRequest
import kotlin.time.Instant

/**
 * Converts a [CreateByEmailRequest] DTO into a domain-layer [CreateUserParams] holder.
 *
 * Performs strict validation and parsing for string values (e.g. enum wire strings,
 * [PermissionCode] instances, and [FileId] UUID strings).
 *
 * @receiver [CreateByEmailRequest] HTTP request body payload.
 * @return [CreateUserParams] domain model containing validated and mapped values.
 * @throws [RequestHandlingException] if any string field cannot be parsed into its domain representation.
 */
public fun CreateByEmailRequest.toCreateUserParams(): CreateUserParams {
    val userRole = UserRole.fromValueOrNull(role) ?: throw RequestHandlingException(
        CommonError.InvalidParameterValue(UserApiFields.ROLE)
    )

    val userAccountStatus = UserAccountStatus.fromValueOrNull(status) ?: throw RequestHandlingException(
        CommonError.InvalidParameterValue(UserApiFields.ACCOUNT_STATUS)
    )

    val parsedAvatarFileId = avatarFileId?.let { fileIdStr ->
        fileIdStr.toFileIdOrNull() ?: throw RequestHandlingException(
            CommonError.InvalidParameterValue(UserApiFields.AVATAR_FILE_ID)
        )
    }

    val parsedBirthdate = birthdate?.let { birthdateMillis ->
        Instant.fromEpochMilliseconds(birthdateMillis)
    }

    return CreateUserParams(
        email = email,
        password = password,
        role = userRole,
        accountStatus = userAccountStatus,
        authorityLevel = authorityLevel,
        permissions = permissionCodes.mapToSet { permissionCodeStr -> PermissionCode(permissionCodeStr) },
        storageConfiguration = storageConfiguration?.toUserStorageConfiguration(),
        nickname = nickname,
        displayName = displayName,
        avatarFileId = parsedAvatarFileId,
        birthdate = parsedBirthdate
    )
}
