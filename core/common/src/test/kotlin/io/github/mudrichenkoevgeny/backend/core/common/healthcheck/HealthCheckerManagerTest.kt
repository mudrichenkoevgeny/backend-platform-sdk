package io.github.mudrichenkoevgeny.backend.core.common.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HealthCheckerManagerTest {

    @Test
    fun `verifyCriticalHealth throws when any critical check fails`() {
        val failingCheck = mockHealthCheck(
            severity = HealthCheckSeverity.CRITICAL,
            result = AppSystemResult.Error(internalError())
        )
        val appLogger: AppLogger = mockk {
            every { logError(any()) } just Runs
        }
        val manager = HealthCheckerManager(setOf(failingCheck), appLogger)

        assertThrows(RuntimeException::class.java) {
            manager.verifyCriticalHealth()
        }

        verify { appLogger.logError(any()) }
    }

    @Test
    fun `verifyCriticalHealth completes when all critical checks succeed`() {
        val okCheck = mockHealthCheck(
            severity = HealthCheckSeverity.CRITICAL,
            result = AppSystemResult.Success(Unit)
        )
        val appLogger: AppLogger = mockk(relaxed = true)
        val manager = HealthCheckerManager(setOf(okCheck), appLogger)

        manager.verifyCriticalHealth()

        verify(exactly = 0) { appLogger.logError(any()) }
    }

    @Test
    fun `checkNonCriticalHealth logs non critical failures`() = runBlocking {
        val error = internalError()
        val nonCriticalCheck = mockHealthCheck(
            severity = HealthCheckSeverity.NON_CRITICAL,
            result = AppSystemResult.Error(error)
        )
        val appLogger: AppLogger = mockk {
            every { logError(error) } just Runs
        }
        val manager = HealthCheckerManager(setOf(nonCriticalCheck), appLogger)

        manager.checkNonCriticalHealth()

        verify { appLogger.logError(error) }
    }

    @Test
    fun `checkNonCriticalHealth completes when all non critical checks succeed`() = runBlocking {
        val okCheck = mockHealthCheck(
            severity = HealthCheckSeverity.NON_CRITICAL,
            result = AppSystemResult.Success(Unit)
        )
        val appLogger: AppLogger = mockk(relaxed = true)
        val manager = HealthCheckerManager(setOf(okCheck), appLogger)

        manager.checkNonCriticalHealth()

        verify(exactly = 0) { appLogger.logError(any()) }
    }

    private fun mockHealthCheck(
        severity: HealthCheckSeverity,
        result: AppSystemResult<Unit>
    ): HealthCheck = mockk {
        every { this@mockk.severity } returns severity
        coEvery { check() } returns result
    }

    private fun internalError(): CommonError.Internal = CommonError.Internal(
        throwable = RuntimeException("health failed"),
        call = null
    )
}

