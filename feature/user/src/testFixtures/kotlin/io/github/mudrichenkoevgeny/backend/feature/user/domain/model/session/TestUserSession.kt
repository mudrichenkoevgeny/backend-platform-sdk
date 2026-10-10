package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.client.createTestClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.session.toUserSessionPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.session.toUserSessionSummary
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

fun createTestUserSessionPrivate(
    id: UserSessionId = UserSessionId.generate(),
    userId: UserId = UserId.generate(),
    userRole: UserRole = UserRole.USER,
    identifier: String = "test@example.com",
    identifierId: UserIdentifierId = UserIdentifierId.generate(),
    identifierDisplayName: String = identifier,
    identifierAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    clientInfo: ClientInfo = createTestClientInfo(),
    expiresAt: Instant = Clock.System.now() + 30.days,
    lastAccessedAt: Instant = Clock.System.now(),
    lastReauthenticatedAt: Instant = Clock.System.now(),
    isSensitiveValuesMasked: Boolean = false,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
): UserSessionPrivate = createTestUserSessionInternal(
    id = id,
    userId = userId,
    userRole = userRole,
    identifier = identifier,
    identifierId = identifierId,
    identifierDisplayName = identifierDisplayName,
    identifierAuthProvider = identifierAuthProvider,
    clientDeviceInfo = clientInfo.clientDeviceInfo,
    userAgent = clientInfo.userAgent,
    ipAddress = clientInfo.ipAddress,
    expiresAt = expiresAt,
    lastAccessedAt = lastAccessedAt,
    lastReauthenticatedAt = lastReauthenticatedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
).toUserSessionPrivate().copy(isSensitiveValuesMasked = isSensitiveValuesMasked)

fun createTestUserSessionSummary(
    id: UserSessionId = UserSessionId.generate(),
    userId: UserId = UserId.generate(),
    userRole: UserRole = UserRole.USER,
    identifier: String = "test@example.com",
    identifierId: UserIdentifierId = UserIdentifierId.generate(),
    identifierDisplayName: String = identifier,
    identifierAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    clientInfo: ClientInfo = createTestClientInfo(),
    expiresAt: Instant = Clock.System.now() + 30.days,
    lastAccessedAt: Instant = Clock.System.now(),
    lastReauthenticatedAt: Instant = Clock.System.now(),
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
): UserSessionSummary = createTestUserSessionInternal(
    id = id,
    userId = userId,
    userRole = userRole,
    identifier = identifier,
    identifierId = identifierId,
    identifierDisplayName = identifierDisplayName,
    identifierAuthProvider = identifierAuthProvider,
    clientDeviceInfo = clientInfo.clientDeviceInfo,
    userAgent = clientInfo.userAgent,
    ipAddress = clientInfo.ipAddress,
    expiresAt = expiresAt,
    lastAccessedAt = lastAccessedAt,
    lastReauthenticatedAt = lastReauthenticatedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
).toUserSessionSummary()
