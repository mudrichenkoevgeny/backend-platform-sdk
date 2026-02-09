package io.github.mudrichenkoevgeny.backend.feature.user.mapper.user

import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.user.CurrentUserResponse
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.user.PublicUserResponse

fun User.toCurrentUserResponse(): CurrentUserResponse = CurrentUserResponse(
    id = id.asHexDashString(),
    role = role.serialName,
    accountStatus = accountStatus.serialName,
    lastLoginAt = lastLoginAt?.toEpochMilli(),
    lastActiveAt = lastActiveAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli()
)

fun User.toPublicUserResponse(): PublicUserResponse = PublicUserResponse(
    id = id.asHexDashString(),
    role = role.serialName,
    accountStatus = accountStatus.serialName
)