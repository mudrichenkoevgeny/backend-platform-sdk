/*
package io.github.mudrichenkoevgeny.backend.core.common.logs

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.Logger
import java.util.UUID

// todo add tests
class AppLoggerTest {

    private val systemLogger = mockk<Logger>(relaxed = true)
    private val businessLogger = mockk<Logger>(relaxed = true)
    private val appLogger = AppLoggerImpl(systemLogger, businessLogger)

    @ParameterizedTest
    @EnumSource(AppErrorSeverity::class)
    fun `logBusinessError should call correct logger level`(severity: AppErrorSeverity) {
        // GIVEN
        val error = getError(severity)

        // WHEN
        appLogger.logBusinessError(error)

        // THEN
        when (severity) {
            AppErrorSeverity.LOW -> verify { businessLogger.info(any()) }
            AppErrorSeverity.MEDIUM -> verify { businessLogger.warn(any()) }
            AppErrorSeverity.HIGH -> verify { businessLogger.error(any()) }
        }
    }

    @Test
    fun `logSystemError should call error logger with correct message`() {
        // GIVEN
        val errorId = ErrorId(UUID.randomUUID())
        val path = "/test-path"
        val method = "GET"
        val throwable = RuntimeException("error")
        val call = mockk<ApplicationCall> {
            every { request.path() } returns path
            every { request.httpMethod.value } returns method
        }

        val systemLogger = mockk<Logger>(relaxed = true)
        val businessLogger = mockk<Logger>(relaxed = true)
        val logger = AppLoggerImpl(systemLogger, businessLogger)

        // WHEN
        logger.logSystemError(errorId, throwable, call)

        // THEN
        verify {
            systemLogger.error(
                match { it.contains("errorId=${errorId.value}")
                        && it.contains(path)
                        && it.contains(method) },
                throwable
            )
        }
    }

    @Test
    fun `logBusinessError LOW severity should not call warn or error`() {
        // GIVEN
        val error = getError(AppErrorSeverity.LOW)

        val systemLogger = mockk<Logger>(relaxed = true)
        val businessLogger = mockk<Logger>(relaxed = true)
        val logger = AppLoggerImpl(systemLogger, businessLogger)

        // WHEN
        logger.logBusinessError(error)

        // THEN
        verify(exactly = 0) { businessLogger.warn(any()) }
        verify(exactly = 0) { businessLogger.error(any()) }
    }

    @Test
    fun `logBusinessError MEDIUM severity should not call info or error`() {
        // GIVEN
        val error = getError(AppErrorSeverity.MEDIUM)

        val systemLogger = mockk<Logger>(relaxed = true)
        val businessLogger = mockk<Logger>(relaxed = true)
        val logger = AppLoggerImpl(systemLogger, businessLogger)

        // WHEN
        logger.logBusinessError(error)

        // THEN
        verify(exactly = 0) { businessLogger.info(any()) }
        verify(exactly = 0) { businessLogger.error(any()) }
    }

    @Test
    fun `logBusinessError HIGH severity should not call info or warn`() {
        // GIVEN
        val error = getError(AppErrorSeverity.HIGH)

        val systemLogger = mockk<Logger>(relaxed = true)
        val businessLogger = mockk<Logger>(relaxed = true)
        val logger = AppLoggerImpl(systemLogger, businessLogger)

        // WHEN
        logger.logBusinessError(error)

        // THEN
        verify(exactly = 0) { businessLogger.info(any()) }
        verify(exactly = 0) { businessLogger.warn(any()) }
    }

    private fun getError(severity: AppErrorSeverity): AppError = mockk {
        every { appErrorSeverity } returns severity
        every { errorId } returns ErrorId(UUID.randomUUID())
        every { code } returns ""
        every { httpStatusCode } returns HttpStatusCode.InternalServerError
        every { publicArgs } returns null
        every { secretArgs } returns null
    }
}*/
