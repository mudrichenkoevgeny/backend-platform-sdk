package io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64

class RefreshTokenProviderImplTest {

    private val provider = RefreshTokenProviderImpl()

    @Test
    fun `getRefreshToken returns opaque token containing two uuid parts`() {
        val result = provider.getRefreshToken()

        assertTrue(result is AppResult.Success)
        val token = (result as AppResult.Success).data.value

        val parts = token.split('.')
        assertEquals(2, parts.size)
        assertTrue(parts[0].isNotBlank())
        assertTrue(parts[1].isNotBlank())
    }

    @Test
    fun `getRefreshTokenHash returns base64 sha256 hash`() {
        val token = RefreshToken("token-value")

        val result = provider.getRefreshTokenHash(token)

        assertTrue(result is AppResult.Success)

        val expected = sha256Base64(token.value)
        assertEquals(expected, (result as AppResult.Success).data.value)
    }

    private fun sha256Base64(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(value.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }
}

