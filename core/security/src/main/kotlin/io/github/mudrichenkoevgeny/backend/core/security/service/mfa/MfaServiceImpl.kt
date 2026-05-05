package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Redis-backed [MfaService] implementation.
 *
 * Manages multifactor authentication challenges by storing serialized [MfaChallengeData] in Redis.
 * Each challenge is identified by a high-entropy UUID token. The lifetime of challenges is
 * dynamically managed via [SecuritySettingsProvider]. Cross-usage between different
 * [MfaChallengeType] contexts is strictly prohibited during retrieval.
 */
@Singleton
class MfaServiceImpl @Inject constructor(
    private val redisManager: RedisManager,
    private val securitySettingsProvider: SecuritySettingsProvider
) : MfaService {

    override suspend fun createChallenge(
        userId: String,
        userRole: String,
        type: MfaChallengeType,
        identifierId: String?,
        sessionId: String?,
        metadata: Map<String, String>?
    ): AppResult<MfaChallengeData> {
        val token = Uuid.random().toString()
        val expirationSeconds = securitySettingsProvider.getMfaTokenExpirationSeconds()

        val data = MfaChallengeData(
            token = token,
            userId = userId,
            userRole = userRole,
            identifierId = identifierId,
            sessionId = sessionId,
            type = type,
            metadata = metadata ?: emptyMap()
        )

        val serializedData = FoundationJson.encodeToString(data)

        return redisManager.setWithExpiration(
            key = buildKey(token),
            value = serializedData,
            expirationSeconds = expirationSeconds.toLong()
        ).mapSuccess { data }
    }

    override suspend fun getChallenge(
        token: String,
        type: MfaChallengeType
    ): AppResult<MfaChallengeData> {
        val key = buildKey(token)
        val result = redisManager.get(key)

        val serializedData = when (result) {
            is AppResult.Success -> result.data
                ?: return AppResult.Error(SecurityError.MfaTokenExpired())
            is AppResult.Error -> return result
        }

        return try {
            val data = FoundationJson.decodeFromString<MfaChallengeData>(serializedData)

            if (data.type != type) {
                return AppResult.Error(SecurityError.InvalidMfaToken())
            }

            AppResult.Success(data)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }

    override suspend fun consumeChallenge(token: String): AppResult<Unit> {
        return redisManager.delete(buildKey(token))
    }

    override suspend fun validateChallenge(
        token: String,
        type: MfaChallengeType,
        userId: String,
        sessionId: String?
    ): AppResult<Unit> {
        val challengeResult = getChallenge(token, type)

        val challenge = when (challengeResult) {
            is AppResult.Success -> challengeResult.data
            is AppResult.Error -> return challengeResult
        }

        if (challenge.userId != userId || challenge.sessionId != sessionId) {
            return AppResult.Error(SecurityError.InvalidMfaToken())
        }

        return consumeChallenge(token)
    }

    private fun buildKey(token: String): String {
        return "mfa_challenge:$token"
    }
}