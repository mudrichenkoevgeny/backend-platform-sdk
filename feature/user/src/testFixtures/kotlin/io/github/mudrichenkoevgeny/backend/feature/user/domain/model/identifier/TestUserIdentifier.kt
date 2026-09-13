package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.identifier

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Clock
import kotlin.time.Instant

fun createTestUserIdentifier(
    id: UserIdentifierId = UserIdentifierId.generate(),
    userId: UserId = UserId.generate(),
    userAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    identifier: String = "test@example.com",
    externalProviderEmail: String? = null,
    isSensitiveValuesMasked: Boolean = false,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
) = UserIdentifier(
    id = id,
    userId = userId,
    userAuthProvider = userAuthProvider,
    identifier = identifier,
    externalProviderEmail = externalProviderEmail,
    isSensitiveValuesMasked = isSensitiveValuesMasked,
    createdAt = createdAt,
    updatedAt = updatedAt
)
