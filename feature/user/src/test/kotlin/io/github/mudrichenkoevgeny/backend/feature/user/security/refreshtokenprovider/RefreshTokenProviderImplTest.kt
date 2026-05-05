package io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64

class RefreshTokenProviderImplTest {

    private val provider = RefreshTokenProviderImpl()

    @Test
    fun `getRefreshToken should return unique tokens in correct format`() {
        val result1 = provider.getRefreshToken()
        val result2 = provider.getRefreshToken()

        assertTrue(result1 is AppResult.Success)
        assertTrue(result2 is AppResult.Success)

        val token1 = (result1 as AppResult.Success).data
        val token2 = (result2 as AppResult.Success).data

        assertNotEquals(token1.value, token2.value)

        val parts = token1.value.split(".")
        assertEquals(2, parts.size)
        assertTrue(token1.value.contains("."))
    }

    @Test
    fun `getRefreshTokenHash should produce correct SHA-256 base64 hash`() {
        val rawToken = "test-token-value"
        val refreshToken = RefreshToken(rawToken)

        val expectedHash = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray())
        )

        val result = provider.getRefreshTokenHash(refreshToken)

        assertTrue(result is AppResult.Success)
        assertEquals(expectedHash, (result as AppResult.Success).data.value)
    }

    @Test
    fun `getRefreshTokenHash should be deterministic`() {
        val refreshToken = RefreshToken("constant-value")

        val result1 = provider.getRefreshTokenHash(refreshToken)
        val result2 = provider.getRefreshTokenHash(refreshToken)

        assertEquals(
            (result1 as AppResult.Success).data.value,
            (result2 as AppResult.Success).data.value
        )
    }
}