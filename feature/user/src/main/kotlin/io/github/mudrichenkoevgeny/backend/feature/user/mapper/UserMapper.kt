package io.github.mudrichenkoevgeny.backend.feature.user.mapper

import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.user.UserResponse

fun User.toResponse(userIdentifiersList: List<UserIdentifier>): UserResponse = UserResponse(
    id = id.asHexDashString(),
    role = role.name,
    accountStatus = accountStatus.name,
    userIdentifiers = userIdentifiersList.map { it.toResponse() },
    lastLoginAt = lastLoginAt?.toEpochMilli(),
    lastActiveAt = lastActiveAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli()
)