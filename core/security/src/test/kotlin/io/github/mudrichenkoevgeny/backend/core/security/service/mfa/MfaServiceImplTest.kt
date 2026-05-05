package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MfaServiceImplTest {

    private val redisManager = mockk<RedisManager>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val service = MfaServiceImpl(redisManager, securitySettingsProvider)

    @Test
    fun `createChallenge saves data to redis and returns it`() = runTest {
        val userId = "user-1"
        val userRole = "USER"
        val type = MfaChallengeType.LOGIN_TOTP
        val expiration = 300

        every { securitySettingsProvider.getMfaTokenExpirationSeconds() } returns expiration
        coEvery { redisManager.setWithExpiration(any(), any(), expiration.toLong()) } returns AppResult.Success(Unit)

        val result = service.createChallenge(userId, userRole, type)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(userId, data.userId)
        assertEquals(type, data.type)

        coVerify {
            redisManager.setWithExpiration(
                key = match { it.startsWith("mfa_challenge:") },
                value = any(),
                expirationSeconds = expiration.toLong()
            )
        }
    }

    @Test
    fun `getChallenge returns data when token exists and type matches`() = runTest {
        val token = "test-token"
        val type = MfaChallengeType.LOGIN_TOTP
        val challengeData = MfaChallengeData(token, "user-1", "USER", type = type)
        val json = FoundationJson.encodeToString(challengeData)

        coEvery { redisManager.get("mfa_challenge:$token") } returns AppResult.Success(json)

        val result = service.getChallenge(token, type)

        assertTrue(result is AppResult.Success)
        assertEquals(challengeData, (result as AppResult.Success).data)
    }

    @Test
    fun `getChallenge returns MfaTokenExpired when redis returns null`() = runTest {
        val token = "expired-token"
        coEvery { redisManager.get("mfa_challenge:$token") } returns AppResult.Success(null)

        val result = service.getChallenge(token, MfaChallengeType.LOGIN_TOTP)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.MfaTokenExpired)
    }

    @Test
    fun `getChallenge returns InvalidMfaToken when types do not match`() = runTest {
        val token = "token"
        val challengeData = MfaChallengeData(token, "u1", "r", type = MfaChallengeType.SETUP_TOTP)
        val json = FoundationJson.encodeToString(challengeData)

        coEvery { redisManager.get("mfa_challenge:$token") } returns AppResult.Success(json)

        val result = service.getChallenge(token, MfaChallengeType.LOGIN_TOTP)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.InvalidMfaToken)
    }

    @Test
    fun `validateChallenge succeeds and consumes token when data matches`() = runTest {
        val token = "valid-token"
        val userId = "user-1"
        val sessionId = "session-1"
        val type = MfaChallengeType.STEP_UP
        val challengeData = MfaChallengeData(token, userId, "USER", type = type, sessionId = sessionId)
        val json = FoundationJson.encodeToString(challengeData)

        coEvery { redisManager.get("mfa_challenge:$token") } returns AppResult.Success(json)
        coEvery { redisManager.delete("mfa_challenge:$token") } returns AppResult.Success(Unit)

        val result = service.validateChallenge(token, type, userId, sessionId)

        assertTrue(result is AppResult.Success)
        coVerify { redisManager.delete("mfa_challenge:$token") }
    }

    @Test
    fun `validateChallenge returns InvalidMfaToken when userId does not match`() = runTest {
        val token = "token"
        val type = MfaChallengeType.LOGIN_TOTP
        val challengeData = MfaChallengeData(token, "real-user", "USER", type = type)
        val json = FoundationJson.encodeToString(challengeData)

        coEvery { redisManager.get("mfa_challenge:$token") } returns AppResult.Success(json)

        val result = service.validateChallenge(token, type, "imposter", null)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.InvalidMfaToken)
    }

    @Test
    fun `consumeChallenge deletes key from redis`() = runTest {
        val token = "token-to-delete"
        coEvery { redisManager.delete("mfa_challenge:$token") } returns AppResult.Success(Unit)

        val result = service.consumeChallenge(token)

        assertTrue(result is AppResult.Success)
        coVerify { redisManager.delete("mfa_challenge:$token") }
    }
}