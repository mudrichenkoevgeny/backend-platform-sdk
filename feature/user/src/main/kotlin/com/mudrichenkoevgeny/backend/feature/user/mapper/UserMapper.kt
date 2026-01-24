package com.mudrichenkoevgeny.backend.feature.user.mapper

import com.mudrichenkoevgeny.backend.feature.user.model.user.User
import com.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import com.mudrichenkoevgeny.backend.feature.user.network.response.user.UserResponse

fun User.toResponse(userIdentifiersList: List<UserIdentifier>): UserResponse = UserResponse(
    id = id.value.toString(),
    role = role.name,
    accountStatus = accountStatus.name,
    userIdentifiers = userIdentifiersList.map { it.toResponse() },
    lastLoginAt = lastLoginAt?.toEpochMilli(),
    lastActiveAt = lastActiveAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli()
)