package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall

/**
 * Reads and validates a required path parameter from the [ApplicationCall].
 *
 * - If the parameter with [name] is missing, throws [RequestHandlingException] with [CommonError.MissingRequiredParameter].
 * - If [mapper] fails to convert the raw string, throws [RequestHandlingException] with [CommonError.InvalidParameterValue].
 *
 * @param name name of the path parameter
 * @param mapper function that converts the raw string value to type [T]
 * @throws RequestHandlingException when the parameter is missing or cannot be converted
 */
inline fun <reified T> ApplicationCall.validatePathParameter(
    name: String,
    mapper: (String) -> T
): T {
    val rawValue = parameters[name]
        ?: throw RequestHandlingException(CommonError.MissingRequiredParameter(name))

    return try {
        mapper(rawValue)
    } catch (_: Exception) {
        throw RequestHandlingException(CommonError.InvalidParameterValue(name))
    }
}