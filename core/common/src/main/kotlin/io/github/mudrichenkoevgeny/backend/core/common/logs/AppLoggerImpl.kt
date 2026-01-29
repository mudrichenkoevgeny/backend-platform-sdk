package io.github.mudrichenkoevgeny.backend.core.common.logs

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BusinessLogger
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.SystemLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.Logger
import javax.inject.Singleton

@Singleton
class AppLoggerImpl(
    @param:SystemLogger private val systemLogger: Logger,
    @param:BusinessLogger private val businessLogger: Logger
) : AppLogger {

    override fun logError(appError: AppError) {
        if (appError is CommonError.System) {
            logSystemError(appError)
        } else {
            logBusinessError(appError)
        }
    }

    private fun logSystemError(systemError: CommonError.System) {
        val parts = mutableListOf("Unhandled exception", "errorId=${systemError.errorId.value}")

        systemError.call?.let {
            parts += "path=${it.request.path()}"
            parts += "method=${it.request.httpMethod.value}"
        }

        val message = parts.joinToString(", ")
        systemLogger.error(message, systemError.throwable)
    }

    private fun logBusinessError(appError: AppError) {
        val message = buildString {
            append("Business error, ")
            append("errorId=${appError.errorId.value}, ")
            append("code=${appError.code}, ")
            append("httpStatus=${appError.httpStatusCode.value}, ")
            append("publicArgs=${appError.publicArgs}, ")
            append("secretArgs=${appError.secretArgs}")
        }

        when (appError.appErrorSeverity) {
            AppErrorSeverity.LOW -> businessLogger.info(message)
            AppErrorSeverity.MEDIUM -> businessLogger.warn(message)
            AppErrorSeverity.HIGH -> businessLogger.error(message)
        }
    }
}
