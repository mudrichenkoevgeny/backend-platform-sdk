package io.github.mudrichenkoevgeny.backend.feature.user.mapper.user

import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.user.CurrentUserResponse
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.user.PublicUserResponse

/**
 * Maps internal [User] model to the shared network response contracts.
 */
fun User.toCurrentUserResponse(): CurrentUserResponse = CurrentUserResponse(
    id = id.asHexDashString(),
    role = role.serialName,
    accountStatus = accountStatus.serialName,
    lastLoginAt = lastLoginAt?.toEpochMilli(),
    lastActiveAt = lastActiveAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli()
)

/**
 * Maps internal [User] model to a public (non-sensitive) shared network response contract.
 */
fun User.toPublicUserResponse(): PublicUserResponse = PublicUserResponse(
    id = id.asHexDashString(),
    role = role.serialName,
    accountStatus = accountStatus.serialName
)