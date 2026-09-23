package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

fun createTestSessionToken(
    sessionId: UserSessionId = UserSessionId.generate(),
    identifierId: UserIdentifierId = UserIdentifierId.generate(),
    accessToken: AccessToken = AccessToken("test_access_token"),
    refreshToken: RefreshToken = RefreshToken("test_refresh_token"),
    expiresAt: Instant = Clock.System.now() + 1.hours
) = SessionToken(
    sessionId = sessionId,
    identifierId = identifierId,
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAt = expiresAt
)

fun createTestAuthData(
    userDetails: UserDetails = createTestUserDetails(),
    sessionToken: SessionToken = createTestSessionToken()
) = AuthData(
    userDetails = userDetails,
    sessionToken = sessionToken
)
