package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.identifier.createTestUserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.session.createTestUserSessionId

fun createTestMfaChallengeData(
    token: String = "test-token",
    userId: String = createTestUserSessionId().asHexDashString(),
    userRole: String = "USER",
    identifierId: String? = createTestUserIdentifierId().asHexDashString(),
    sessionId: String? = createTestUserSessionId().asHexDashString(),
    type: MfaChallengeType = MfaChallengeType.LOGIN_TOTP,
    metadata: Map<String, String> = emptyMap()
) = MfaChallengeData(
    token = token,
    userId = userId,
    userRole = userRole,
    identifierId = identifierId,
    sessionId = sessionId,
    type = type,
    metadata = metadata
)
