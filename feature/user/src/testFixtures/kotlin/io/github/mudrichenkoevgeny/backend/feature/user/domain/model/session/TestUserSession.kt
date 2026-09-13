package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.client.createTestClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

fun createTestUserSession(
    id: UserSessionId = UserSessionId.generate(),
    userId: UserId = UserId.generate(),
    userRole: UserRole = UserRole.USER,
    identifier: String = "test@example.com",
    identifierId: UserIdentifierId = UserIdentifierId.generate(),
    identifierAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    clientInfo: ClientInfo = createTestClientInfo(),
    expiresAt: Instant = Clock.System.now() + 30.days,
    lastAccessedAt: Instant = Clock.System.now(),
    lastReauthenticatedAt: Instant = Clock.System.now(),
    isSensitiveValuesMasked: Boolean = false,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
) = UserSession(
    id = id,
    userId = userId,
    userRole = userRole,
    identifier = identifier,
    identifierId = identifierId,
    identifierAuthProvider = identifierAuthProvider,
    deviceInfo = clientInfo.deviceInfo,
    userAgent = clientInfo.userAgent,
    ipAddress = clientInfo.ipAddress,
    expiresAt = expiresAt,
    lastAccessedAt = lastAccessedAt,
    lastReauthenticatedAt = lastReauthenticatedAt,
    isSensitiveValuesMasked = isSensitiveValuesMasked,
    createdAt = createdAt,
    updatedAt = updatedAt
)
