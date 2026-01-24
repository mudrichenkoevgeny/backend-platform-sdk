package com.mudrichenkoevgeny.backend.feature.user.mapper

import com.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import com.mudrichenkoevgeny.backend.feature.user.network.response.useridentifier.UserIdentifierResponse

fun UserIdentifier.toResponse(): UserIdentifierResponse = UserIdentifierResponse(
    id = id.value.toString(),
    userId = userId.value.toString(),
    userAuthProvider = userAuthProvider.serialName,
    identifier = identifier,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli()
)