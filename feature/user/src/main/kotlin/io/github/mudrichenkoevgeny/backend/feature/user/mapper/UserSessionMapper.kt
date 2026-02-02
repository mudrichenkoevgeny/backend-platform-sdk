package io.github.mudrichenkoevgeny.backend.feature.user.mapper

import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.session.UserSessionResponse

fun UserSession.toResponse(): UserSessionResponse = UserSessionResponse(
    id = id.asHexDashString(),
    identifierId = userIdentifierId.asHexDashString(),
    identifierAuthProvider = userIdentifierAuthProvider.serialName,
    expiresAt = expiresAt.toEpochMilli(),
    userAgent = userAgent,
    ipAddress = ipAddress,
    deviceName = userDeviceName,
    createdAt = createdAt.toEpochMilli(),
    lastAccessedAt = lastAccessedAt.toEpochMilli()
)