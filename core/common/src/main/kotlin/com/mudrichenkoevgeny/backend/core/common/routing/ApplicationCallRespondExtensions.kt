package com.mudrichenkoevgeny.backend.core.common.routing

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: AppResult<T>,
    appLogger: AppLogger,
    appErrorParser: AppErrorParser,
    mapper: (T) -> Any = { it }
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
                appErrorParser.getApiError(result.error)
            )
        }
    }
}