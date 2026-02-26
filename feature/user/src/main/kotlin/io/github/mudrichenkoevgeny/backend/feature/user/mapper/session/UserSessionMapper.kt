package io.github.mudrichenkoevgeny.backend.feature.user.mapper.session

import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.session.UserSessionResponse

fun UserSession.toUserSessionResponse(): UserSessionResponse = UserSessionResponse(
    id = id.asHexDashString(),
    identifierId = userIdentifierId.asHexDashString(),
    identifierAuthProvider = userIdentifierAuthProvider.serialName,
    expiresAt = expiresAt.toEpochMilli(),
    clientType = userClientType?.serialName,
    userAgent = userAgent,
    ipAddress = ipAddress,
    language = language,
    deviceName = userDeviceName,
    appVersion = appVersion,
    operationSystemVersion = operationSystemVersion,
    createdAt = createdAt.toEpochMilli(),
    lastAccessedAt = lastAccessedAt.toEpochMilli()
)