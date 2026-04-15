package io.github.mudrichenkoevgeny.backend.core.common.logs

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.Logger

class AppLoggerTest {

    private val systemLogger = mockk<Logger>(relaxed = true)
    private val businessLogger = mockk<Logger>(relaxed = true)
    private val appLogger = AppLoggerImpl(systemLogger, businessLogger)

    @Test
    fun `logError routes internal error to system logger`() {
        val internalError = CommonError.Internal(
            throwable = RuntimeException("boom"),
            call = null
        )

        appLogger.logError(internalError)

        verify {
            systemLogger.error(
                match<String> { it.contains("Unhandled exception") },
                any<Throwable>()
            )
        }
    }

    @ParameterizedTest
    @EnumSource(AppErrorSeverity::class)
    fun `logError routes business errors to correct level`(severity: AppErrorSeverity) {
        val error = stubBusinessError(severity)

        appLogger.logError(error)

        when (severity) {
            AppErrorSeverity.LOW -> verify { businessLogger.info(any()) }
            AppErrorSeverity.MEDIUM -> verify { businessLogger.warn(any()) }
            AppErrorSeverity.HIGH -> verify { businessLogger.error(any()) }
        }
    }

    private fun stubBusinessError(severity: AppErrorSeverity): AppError = mockk {
        every { appErrorSeverity } returns severity
        every { errorId } returns ErrorId.generate()
        every { code } returns "CODE"
        every { httpStatusCode } returns HttpStatusCode.BadRequest
        every { publicArgs } returns mapOf("k" to "v")
        every { secretArgs } returns mapOf("sk" to "sv")
    }
}
