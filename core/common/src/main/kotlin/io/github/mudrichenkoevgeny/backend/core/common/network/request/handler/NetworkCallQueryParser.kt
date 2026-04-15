package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall
import kotlin.collections.orEmpty

/**
 * First non-blank value for [name] when the client sends duplicate query keys, otherwise the single parameter value.
 */
fun ApplicationCall.firstNonBlankQueryValue(name: String): String? =
    request.queryParameters.getAll(name)?.firstOrNull { it.isNotBlank() }
        ?: request.queryParameters[name]?.takeIf { it.isNotBlank() }

/**
 * Parses a strictly positive integer query parameter, or returns [default] when the parameter is absent.
 *
 * @throws RequestHandlingException with [CommonError.InvalidParameterValue] when present but not a positive integer
 */
fun ApplicationCall.parsePositiveIntQuery(paramName: String, default: Int): Int {
    val raw = firstNonBlankQueryValue(paramName) ?: return default
    val number = raw.toIntOrNull()
        ?: throw RequestHandlingException(CommonError.InvalidParameterValue(paramName))
    if (number < 1) {
        throw RequestHandlingException(CommonError.InvalidParameterValue(paramName))
    }
    return number
}

fun ApplicationCall.getQueryValues(name: String): List<String> = request.queryParameters
    .getAll(name)
    ?.filter { it.isNotBlank() }
    .orEmpty()