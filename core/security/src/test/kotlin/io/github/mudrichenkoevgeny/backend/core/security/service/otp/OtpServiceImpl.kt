package io.github.mudrichenkoevgeny.backend.core.security.service.otp

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OtpServiceImplTest {

    private val redisManager: RedisManager = mockk()
    private val securitySettingsProvider: SecuritySettingsProvider = mockk()
    private val service = OtpServiceImpl(
        redisManager = redisManager,
        securitySettingsProvider = securitySettingsProvider
    )

    private val otpConfirmation = OtpConfirmation(
        expirationSeconds = 300,
        retryAfterSeconds = 60,
        numberOfSymbols = 6
    )

    @BeforeEach
    fun setup() {
        every { securitySettingsProvider.getOtpConfirmation() } returns otpConfirmation
    }

    @Test
    fun `getOtp returns existing code when cooling-off period passed`() = runTest {
        val ttl = (otpConfirmation.expirationSeconds - otpConfirmation.retryAfterSeconds - 10).toLong()

        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)
        coEvery { redisManager.getTtl(KEY) } returns AppResult.Success(ttl)

        val result = service.getOtp(IDENTIFIER, TYPE)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(EXISTING_CODE, data.code)
        assertEquals(otpConfirmation, data.otpConfirmation)
        coVerify(exactly = 0) { redisManager.setWithExpiration(any(), any(), any()) }
    }

    @Test
    fun `getOtp returns OtpRetryTooSoon when requested too early`() = runTest {
        val ttl = (otpConfirmation.expirationSeconds - 10).toLong()

        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)
        coEvery { redisManager.getTtl(KEY) } returns AppResult.Success(ttl)

        val result = service.getOtp(IDENTIFIER, TYPE)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is SecurityError.OtpRetryTooSoon)
        assertEquals(50, (error as SecurityError.OtpRetryTooSoon).publicArgs?.get("retryAfterSeconds"))
    }

    @Test
    fun `getOtp generates and saves new code when missing in redis`() = runTest {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(null)
        coEvery { redisManager.setWithExpiration(KEY, any(), otpConfirmation.expirationSeconds.toLong()) } returns AppResult.Success(Unit)

        val result = service.getOtp(IDENTIFIER, TYPE)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(6, data.code.length)
        assertTrue(data.code.all { it.isDigit() })

        coVerify(exactly = 1) {
            redisManager.setWithExpiration(KEY, data.code, otpConfirmation.expirationSeconds.toLong())
        }
    }

    @Test
    fun `verifyOtp returns true and deletes key on success when deleteOnSuccess is true`() = runTest {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)
        coEvery { redisManager.delete(KEY) } returns AppResult.Success(Unit)

        val result = service.verifyOtp(IDENTIFIER, TYPE, EXISTING_CODE, deleteOnSuccess = true)

        assertTrue(result is AppResult.Success)
        assertEquals(true, (result as AppResult.Success).data)
        coVerify(exactly = 1) { redisManager.delete(KEY) }
    }

    @Test
    fun `verifyOtp returns true but does not delete key when deleteOnSuccess is false`() = runTest {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)

        val result = service.verifyOtp(IDENTIFIER, TYPE, EXISTING_CODE, deleteOnSuccess = false)

        assertTrue(result is AppResult.Success)
        assertEquals(true, (result as AppResult.Success).data)
        coVerify(exactly = 0) { redisManager.delete(any()) }
    }

    @Test
    fun `verifyOtp returns false when code is incorrect`() = runTest {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(EXISTING_CODE)

        val result = service.verifyOtp(IDENTIFIER, TYPE, "wrong", deleteOnSuccess = true)

        assertTrue(result is AppResult.Success)
        assertEquals(false, (result as AppResult.Success).data)
        coVerify(exactly = 0) { redisManager.delete(any()) }
    }

    @Test
    fun `verifyOtp returns false when code is missing in redis`() = runTest {
        coEvery { redisManager.get(KEY) } returns AppResult.Success(null)

        val result = service.verifyOtp(IDENTIFIER, TYPE, EXISTING_CODE, deleteOnSuccess = true)

        assertTrue(result is AppResult.Success)
        assertEquals(false, (result as AppResult.Success).data)
    }

    private companion object {
        const val IDENTIFIER = "test-user"
        val TYPE = OtpVerificationType("EMAIL_VERIFICATION")
        const val KEY = "otp:email_verification:test-user"
        const val EXISTING_CODE = "654321"
    }
}