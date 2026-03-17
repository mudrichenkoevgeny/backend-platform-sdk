package io.github.mudrichenkoevgeny.backend.feature.user.mapper.useridentifier

import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.useridentifier.UserIdentifierResponse

/**
 * Maps internal [UserIdentifier] to the shared network response contract.
 */
fun UserIdentifier.toUserIdentifierResponse(): UserIdentifierResponse = UserIdentifierResponse(
    id = id.asHexDashString(),
    userId = userId.asHexDashString(),
    userAuthProvider = userAuthProvider.serialName,
    identifier = identifier,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli()
)