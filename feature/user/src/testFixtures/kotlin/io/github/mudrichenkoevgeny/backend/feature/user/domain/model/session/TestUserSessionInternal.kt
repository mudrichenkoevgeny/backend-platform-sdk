package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.client.createTestClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

fun createTestUserSessionInternal(
    id: UserSessionId = UserSessionId.generate(),
    userId: UserId = UserId.generate(),
    userRole: UserRole = UserRole.USER,
    identifier: String = "test@example.com",
    identifierId: UserIdentifierId = UserIdentifierId.generate(),
    identifierDisplayName: String = identifier,
    identifierAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    refreshTokenHash: RefreshTokenHash = RefreshTokenHash("hash_" + id.asHexDashString()),
    deviceInfo: ClientDeviceInfo = createTestClientInfo().deviceInfo,
    userAgent: String? = "Mozilla/5.0",
    ipAddress: String? = "127.0.0.1",
    expiresAt: Instant = Clock.System.now() + 30.days,
    lastAccessedAt: Instant = Clock.System.now(),
    lastReauthenticatedAt: Instant = Clock.System.now(),
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
) = UserSessionInternal(
    id = id,
    userId = userId,
    userRole = userRole,
    identifier = identifier,
    identifierId = identifierId,
    identifierDisplayName = identifierDisplayName,
    identifierAuthProvider = identifierAuthProvider,
    refreshTokenHash = refreshTokenHash,
    deviceInfo = deviceInfo,
    userAgent = userAgent,
    ipAddress = ipAddress,
    expiresAt = expiresAt,
    lastAccessedAt = lastAccessedAt,
    lastReauthenticatedAt = lastReauthenticatedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
