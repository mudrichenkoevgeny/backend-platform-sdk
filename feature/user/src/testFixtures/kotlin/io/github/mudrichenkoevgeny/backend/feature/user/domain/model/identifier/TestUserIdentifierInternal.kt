package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.identifier

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Clock
import kotlin.time.Instant

fun createTestUserIdentifierInternal(
    id: UserIdentifierId = UserIdentifierId.generate(),
    userId: UserId = UserId.generate(),
    userAuthProvider: UserAuthProvider = UserAuthProvider.EMAIL,
    identifier: String = "test@example.com",
    passwordHash: PasswordHash? = PasswordHash("hashed_password"),
    externalProviderEmail: String? = null,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
) = UserIdentifierInternal(
    id = id,
    userId = userId,
    userAuthProvider = userAuthProvider,
    identifier = identifier,
    passwordHash = passwordHash,
    externalProviderEmail = externalProviderEmail,
    createdAt = createdAt,
    updatedAt = updatedAt
)
