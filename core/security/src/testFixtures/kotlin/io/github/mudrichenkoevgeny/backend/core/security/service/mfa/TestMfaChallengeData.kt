package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.common.domain.model.user.createTestUserId
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.common.domain.model.user.createTestUserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.common.domain.model.user.createTestUserSessionId

fun createTestMfaChallengeData(
    token: String = "test-token",
    userId: UserId = createTestUserId(),
    userRole: UserRole = UserRole.USER,
    identifierId: UserIdentifierId? = createTestUserIdentifierId(),
    sessionId: UserSessionId? = createTestUserSessionId(),
    type: MfaChallengeType = MfaChallengeType.LOGIN_TOTP,
    metadata: Map<String, String> = emptyMap()
): MfaChallengeData = MfaChallengeData(
    token = token,
    userId = userId,
    userRole = userRole,
    identifierId = identifierId,
    sessionId = sessionId,
    type = type,
    metadata = metadata
)
