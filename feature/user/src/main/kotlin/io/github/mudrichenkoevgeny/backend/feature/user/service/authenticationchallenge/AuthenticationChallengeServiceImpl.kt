package io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Default [AuthenticationChallengeService] implementation based on [SecurityConfig].
 */
@Singleton
class AuthenticationChallengeServiceImpl @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val mfaService: MfaService,
) : AuthenticationChallengeService {

    override suspend fun ensureSessionConfirmed(
        userDetails: UserDetails,
        userSession: UserSessionInternal
    ): AppResult<Unit> {
        val validitySeconds = if (userDetails.role == UserRole.USER) {
            securitySettingsProvider.getRecentAuthenticationValidityInSeconds()
        } else {
            securitySettingsProvider.getRecentAuthenticationValidityInSecondsForManagement()
        }

        if (isWithinValidityWindow(userSession.lastReauthenticatedAt, validitySeconds)) {
            return AppResult.Success(Unit)
        }

        return createChallenge(
            userId = userDetails.id.asHexDashString(),
            userRole = userDetails.role.serialName,
            identifierId = userSession.identifierId.asHexDashString(),
            sessionId = userSession.id.asHexDashString()
        )
    }

    private fun isWithinValidityWindow(lastReauthenticatedAt: Instant?, validitySeconds: Int): Boolean {
        val at = lastReauthenticatedAt ?: return false
        val validityEnd = at + validitySeconds.seconds
        return validityEnd > Clock.System.now()
    }

    private suspend fun createChallenge(
        userId: String,
        userRole: String,
        identifierId: String,
        sessionId: String
    ): AppResult<Unit> {
        val mfaResult = mfaService.createChallenge(
            userId = userId,
            userRole = userRole,
            type = MfaChallengeType.STEP_UP,
            identifierId = identifierId,
            sessionId = sessionId
        )

        return when (mfaResult) {
            is AppResult.Error -> AppResult.Error(mfaResult.error)
            is AppResult.Success -> AppResult.Error(
                SecurityError.TotpConfirmationRequired(mfaToken = mfaResult.data.token)
            )
        }
    }
}
