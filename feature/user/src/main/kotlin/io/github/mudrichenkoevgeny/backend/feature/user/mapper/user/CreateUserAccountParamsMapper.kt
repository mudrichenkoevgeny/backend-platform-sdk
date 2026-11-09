package io.github.mudrichenkoevgeny.backend.feature.user.mapper.user

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.CreateUserAccountParams
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.CreateUserParams

/**
 * Converts a [CreateUserParams] holder into a [CreateUserAccountParams] profile holder.
 *
 * @receiver [CreateUserParams] containing email, password, and account profile details.
 * @return [CreateUserAccountParams] containing user account profile details without credentials.
 */
public fun CreateUserParams.toUserAccountParams(): CreateUserAccountParams = CreateUserAccountParams(
    role = role,
    accountStatus = accountStatus,
    authorityLevel = authorityLevel,
    permissions = permissions,
    storageConfiguration = storageConfiguration,
    nickname = nickname,
    displayName = displayName,
    avatarFileId = avatarFileId,
    birthdate = birthdate
)
