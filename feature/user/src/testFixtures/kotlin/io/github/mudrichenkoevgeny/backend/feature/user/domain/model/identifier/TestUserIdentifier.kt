package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.identifier

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierSummary
import kotlin.time.Clock
import kotlin.time.Instant

fun createTestUserIdentifierPrivate(
    id: UserIdentifierId = UserIdentifierId.generate(),
    userId: UserId = UserId.generate(),
    userAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    identifier: String = "test@example.com",
    displayName: String = identifier,
    externalProviderEmail: String? = null,
    isSensitiveValuesMasked: Boolean = false,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
): UserIdentifierPrivate = createTestUserIdentifierInternal(
    id = id,
    userId = userId,
    userAuthProvider = userAuthProvider,
    identifier = identifier,
    displayName = displayName,
    externalProviderEmail = externalProviderEmail,
    createdAt = createdAt,
    updatedAt = updatedAt
).toUserIdentifierPrivate().copy(isSensitiveValuesMasked = isSensitiveValuesMasked)

fun createTestUserIdentifierSummary(
    id: UserIdentifierId = UserIdentifierId.generate(),
    userId: UserId = UserId.generate(),
    userAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    identifier: String = "test@example.com",
    displayName: String = identifier,
    externalProviderEmail: String? = null,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
): UserIdentifierSummary = createTestUserIdentifierInternal(
    id = id,
    userId = userId,
    userAuthProvider = userAuthProvider,
    identifier = identifier,
    displayName = displayName,
    externalProviderEmail = externalProviderEmail,
    createdAt = createdAt,
    updatedAt = updatedAt
).toUserIdentifierSummary()
