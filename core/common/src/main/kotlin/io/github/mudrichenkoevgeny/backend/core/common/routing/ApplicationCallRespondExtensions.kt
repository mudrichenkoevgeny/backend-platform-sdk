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
 * Behavior:
 * - For [AppResult.Success]:
 *   - Responds with [successStatus] (defaults to `204 No Content` for `Unit`, or `200 OK` for data).
 * - For [AppResult.Error]:
 *   - Logs the error via [appLogger].
 *   - Responds with the error's `httpStatusCode` and serialized API error body from [appErrorParser].
 */
suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: AppResult<T>,
    appLogger: AppLogger,
    appErrorParser: AppErrorParser,
    successStatus: HttpStatusCode = if (T::class == Unit::class) {
        HttpStatusCode.NoContent
    } else {
        HttpStatusCode.OK
    }
) {
    when (result) {
        is AppResult.Success -> {
            if (T::class == Unit::class && successStatus == HttpStatusCode.NoContent) {
                respond(HttpStatusCode.NoContent)
            } else if (T::class == Unit::class) {
                respond(successStatus)
            } else {
                respond(successStatus, result.data)
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
 * Behavior:
 * - For [AppResult.Success]:
 *   - Responds with [successStatus] (defaults to `204 No Content` for `Unit`, or `200 OK` with mapped body).
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
    successStatus: HttpStatusCode = if (T::class == Unit::class) {
        HttpStatusCode.NoContent
    } else {
        HttpStatusCode.OK
    },
    crossinline mapper: (T) -> R
) {
    when (result) {
        is AppResult.Success -> {
            if (T::class == Unit::class && successStatus == HttpStatusCode.NoContent) {
                respond(HttpStatusCode.NoContent)
            } else if (T::class == Unit::class) {
                respond(successStatus)
            } else {
                respond(
                    status = successStatus,
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
