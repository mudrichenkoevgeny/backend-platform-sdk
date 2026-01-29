package io.github.mudrichenkoevgeny.backend.core.common.application.statuspages

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.validation.ValidationException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.response.respond

fun Application.configureStatusPages(
    appErrorParser: AppErrorParser,
    appLogger: AppLogger
) {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            val appError = cause.error
            appLogger.logError(appError)
            val apiError = appErrorParser.getApiError(appError)
            call.respond(cause.error.httpStatusCode, apiError)
        }

        exception<ContentTransformationException> { call, cause ->
            val appError = CommonError.InvalidJsonBody(cause.message)
            appLogger.logError(appError)
            val apiError = appErrorParser.getApiError(appError)
            call.respond(appError.httpStatusCode, apiError)
        }

        exception<BadRequestException> { call, cause ->
            val appError = CommonError.BadRequest(cause.message)
            appLogger.logError(appError)
            val apiError = appErrorParser.getApiError(appError)
            call.respond(appError.httpStatusCode, apiError)
        }

        exception<Throwable> { call, cause ->
            val appError = CommonError.System(cause, call)
            appLogger.logError(appError)
            val apiError = appErrorParser.getApiError(appError)
            call.respond(appError.httpStatusCode, apiError)
        }
    }
}