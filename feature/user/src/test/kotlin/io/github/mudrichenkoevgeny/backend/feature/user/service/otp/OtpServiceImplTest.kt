package io.github.mudrichenkoevgeny.backend.feature.user.service.otp

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OtpServiceImplTest {

    private val redisManager: RedisManager = mockk()
    private val service = OtpServiceImpl(redisManager = redisManager)

    @Test
    fun `getOtp returns existing code without overwriting`() = runBlocking {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)

        val result = service.getOtp(identifier = IDENTIFIER, type = TYPE, expirationSeconds = EXPIRATION_SECONDS)

        assertTrue(result is AppResult.Success)
        assertEquals(EXISTING_CODE, (result as AppResult.Success).data)
        coVerify(exactly = 0) { redisManager.setWithExpiration(any(), any(), any()) }
    }

    @Test
    fun `getOtp generates new code and saves when missing`() = runBlocking {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(null)
        coEvery { redisManager.setWithExpiration(KEY, any(), EXPIRATION_SECONDS) } returns AppResult.Success(Unit)

        val result = service.getOtp(identifier = IDENTIFIER, type = TYPE, expirationSeconds = EXPIRATION_SECONDS)

        assertTrue(result is AppResult.Success)
        val code = (result as AppResult.Success).data
        assertTrue(code.length == 6)
        assertTrue(code.all { it.isDigit() })

        coVerify(exactly = 1) { redisManager.setWithExpiration(KEY, code, EXPIRATION_SECONDS) }
    }

    @Test
    fun `verifyOtp returns false for missing or mismatched code`() = runBlocking {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(null)

        val missingResult = service.verifyOtp(identifier = IDENTIFIER, type = TYPE, code = "000000", deleteOnSuccess = true)
        assertTrue(missingResult is AppResult.Success)
        assertEquals(false, (missingResult as AppResult.Success).data)

        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)
        val mismatchResult = service.verifyOtp(identifier = IDENTIFIER, type = TYPE, code = "000000", deleteOnSuccess = true)
        assertTrue(mismatchResult is AppResult.Success)
        assertEquals(false, (mismatchResult as AppResult.Success).data)

        coVerify(exactly = 0) { redisManager.delete(any()) }
    }

    @Test
    fun `verifyOtp deletes key on success when enabled`() = runBlocking {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)
        coEvery { redisManager.delete(KEY) } returns AppResult.Success(Unit)

        val result = service.verifyOtp(identifier = IDENTIFIER, type = TYPE, code = EXISTING_CODE, deleteOnSuccess = true)

        assertTrue(result is AppResult.Success)
        assertEquals(true, (result as AppResult.Success).data)
        coVerify(exactly = 1) { redisManager.delete(KEY) }
    }

    @Test
    fun `verifyOtp does not delete key on success when disabled`() = runBlocking {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)

        val result = service.verifyOtp(identifier = IDENTIFIER, type = TYPE, code = EXISTING_CODE, deleteOnSuccess = false)

        assertTrue(result is AppResult.Success)
        assertEquals(true, (result as AppResult.Success).data)
        coVerify(exactly = 0) { redisManager.delete(any()) }
    }

    private companion object {
        const val IDENTIFIER = "user@example.com"
        val TYPE = OtpVerificationType.EMAIL_VERIFICATION
        const val KEY = "otp:email_verification:$IDENTIFIER"

        const val EXPIRATION_SECONDS = 300L
        const val EXISTING_CODE = "123456"
    }
}

