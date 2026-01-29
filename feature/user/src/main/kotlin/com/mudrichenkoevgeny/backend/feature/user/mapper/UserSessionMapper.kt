package io.github.mudrichenkoevgeny.backend.feature.user.mapper

import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.backend.feature.user.network.response.session.UserSessionResponse

fun UserSession.toResponse(): UserSessionResponse = UserSessionResponse(
    id = id.value.toString(),
    identifierId = userIdentifierId.value.toString(),
    identifierAuthProvider = userIdentifierAuthProvider.serialName,
    expiresAt = expiresAt.toEpochMilli(),
    userAgent = userAgent,
    ipAddress = ipAddress,
    deviceName = userDeviceName,
    createdAt = createdAt.toEpochMilli(),
    lastAccessedAt = lastAccessedAt.toEpochMilli()
)