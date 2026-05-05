package io.github.mudrichenkoevgeny.backend.core.common.routing

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * Sends an HTTP response based on the given [AppResult].
 *
 * Behaviour:
 * - For [AppResult.Success]:
 *   - If `T` is `Unit`, responds with `204 No Content`.
 *   - Otherwise responds with `200 OK` and the body from [AppResult.Success.data].
 * - For [AppResult.Error]:
 *   - Logs the error via [appLogger].
 *   - Responds with the error's `httpStatusCode` and serialized API error body from [appErrorParser].
 */
suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: AppResult<T>,
    appLogger: AppLogger,
    appErrorParser: AppErrorParser
) {
    when (result) {
        is AppResult.Success -> {
            if (T::class == Unit::class) {
                respond(HttpStatusCode.NoContent)
            } else {
                respond(HttpStatusCode.OK, result.data)
            }
        }
        is AppResult.Error -> {
            appLogger.logError(result.error)
            respond(
                result.error.httpStatusCode,
                appErrorParser.getApiErrorResponse(result.error)
            )
        }
    }
}

/**
 * Sends an HTTP response based on the given [AppResult].
 *
 * Behaviour:
 * - For [AppResult.Success]:
 *   - If `T` is `Unit`, responds with `204 No Content`.
 *   - Otherwise responds with `200 OK` and the body produced by [mapper].
 * - For [AppResult.Error]:
 *   - Logs the error via [appLogger].
 *   - Responds with the error's `httpStatusCode` and serialized API error body from [appErrorParser].
 *
 * The [mapper] can be used to convert domain models into DTOs.
 */
suspend inline fun <reified T : Any, reified R : Any> ApplicationCall.respondResult(
    result: AppResult<T>,
    appLogger: AppLogger,
    appErrorParser: AppErrorParser,
    crossinline mapper: (T) -> R
) {
    when (result) {
        is AppResult.Success -> {
            if (T::class == Unit::class) {
                respond(HttpStatusCode.NoContent)
            } else {
                respond(
                    status = HttpStatusCode.OK,
                    message = mapper(result.data)
                )
            }
        }
        is AppResult.Error -> {
            appLogger.logError(result.error)
            respond(
                result.error.httpStatusCode,
                appErrorParser.getApiErrorResponse(result.error)
            )
        }
    }
}