package io.github.mudrichenkoevgeny.backend.core.common.application.statuspages

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.response.respond

/**
 * Configures Ktor [StatusPages] to translate exceptions into structured API error responses.
 *
 * Mappings:
 * - [RequestHandlingException] → logs the wrapped [CommonError] and responds with its HTTP status.
 * - [ContentTransformationException] → wraps into [CommonError.InvalidJsonBody] and responds accordingly.
 * - [BadRequestException] → wraps into [CommonError.BadRequest].
 * - Any other [Throwable] → wraps into [CommonError.Internal] including the [ApplicationCall] context.
 */
fun Application.configureStatusPages(
    appErrorParser: AppErrorParser,
    appLogger: AppLogger
) {
    install(StatusPages) {
        exception<RequestHandlingException> { call, cause ->
            val appError = cause.error
            appLogger.logError(appError)
            val apiErrorResponse = appErrorParser.getApiErrorResponse(appError)
            call.respond(cause.error.httpStatusCode, apiErrorResponse)
        }

        exception<ContentTransformationException> { call, cause ->
            val appError = CommonError.InvalidJsonBody(cause.message)
            appLogger.logError(appError)
            val apiErrorResponse = appErrorParser.getApiErrorResponse(appError)
            call.respond(appError.httpStatusCode, apiErrorResponse)
        }

        exception<BadRequestException> { call, cause ->
            val appError = CommonError.BadRequest(cause.message)
            appLogger.logError(appError)
            val apiErrorResponse = appErrorParser.getApiErrorResponse(appError)
            call.respond(appError.httpStatusCode, apiErrorResponse)
        }

        exception<Throwable> { call, cause ->
            val appError = CommonError.Internal(cause, call)
            appLogger.logError(appError)
            val apiErrorResponse = appErrorParser.getApiErrorResponse(appError)
            call.respond(appError.httpStatusCode, apiErrorResponse)
        }
    }
}