package io.github.mudrichenkoevgeny.backend.core.security.passwordhasher

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordHasherImplTest {

    private val appLogger = mockk<AppLogger>(relaxed = true)
    private val hasher = PasswordHasherImpl(appLogger)

    @Test
    fun `hash returns hash that verifies with the same password`() {
        val password = "Sup3r_Str0ng_Pass!"

        val hashResult = hasher.hash(password) as AppResult.Success
        val storedHash = hashResult.data

        val verifyResult = hasher.verify(password, storedHash) as AppResult.Success
        assertTrue(verifyResult.data)
    }

    @Test
    fun `verify returns false for wrong password`() {
        val password = "CorrectPass123!"
        val wrongPassword = "WrongPass123!"

        val hashResult = hasher.hash(password) as AppResult.Success
        val storedHash = hashResult.data

        val verifyResult = hasher.verify(wrongPassword, storedHash) as AppResult.Success
        assertFalse(verifyResult.data)
    }

    @Test
    fun `isPasswordValid returns false when password or hash is missing`() {
        val result1 = hasher.isPasswordValid(null, "hash") as AppResult.Success
        val result2 = hasher.isPasswordValid("pass", null) as AppResult.Success
        val result3 = hasher.isPasswordValid("", "hash") as AppResult.Success
        val result4 = hasher.isPasswordValid("pass", "") as AppResult.Success

        assertFalse(result1.data)
        assertFalse(result2.data)
        assertFalse(result3.data)
        assertFalse(result4.data)
    }

    @Test
    fun `isPasswordValidFakeCheck returns success`() {
        val result = hasher.isPasswordValidFakeCheck("any") as AppResult.Success
        assertEquals(Unit, result.data)
    }
}

