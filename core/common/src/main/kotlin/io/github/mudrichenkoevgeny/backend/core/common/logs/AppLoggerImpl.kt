package io.github.mudrichenkoevgeny.backend.core.common.logs

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BusinessLogger
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.SystemLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.Logger
import javax.inject.Singleton

/**
 * Default [AppLogger] implementation that writes errors to two SLF4J loggers:
 * one for low-level system failures and one for business-level problems.
 */
@Singleton
class AppLoggerImpl(
    @param:SystemLogger private val systemLogger: Logger,
    @param:BusinessLogger private val businessLogger: Logger
) : AppLogger {

    override fun logError(appError: AppError) {
        when (appError) {
            is CommonError.Internal -> {
                logSystemError(appError, appError.throwable, appError.call)
            }
            is CommonError.Database -> {
                logSystemError(appError)
            }
            else -> {
                logBusinessError(appError)
            }
        }
    }

    private fun logSystemError(appError: AppError, throwable: Throwable? = null, call: ApplicationCall? = null) {
        val parts = mutableListOf("Unhandled exception", "errorId=${appError.errorId.asHexDashString()}")

        call?.let {
            parts += "path=${it.request.path()}"
            parts += "method=${it.request.httpMethod.value}"
        }

        val message = parts.joinToString(", ")
        if (throwable != null) {
            systemLogger.error(message, throwable)
        } else {
            systemLogger.error(message)
        }
    }

    private fun logBusinessError(appError: AppError) {
        val message = buildString {
            append("Business error, ")
            append("errorId=${appError.errorId.asHexDashString()}, ")
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
