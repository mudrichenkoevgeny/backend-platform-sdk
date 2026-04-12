package io.github.mudrichenkoevgeny.backend.core.common.validation

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall

/**
 * First non-blank value for [name] when the client sends duplicate query keys, otherwise the single parameter value.
 */
fun ApplicationCall.firstNonBlankQueryValue(name: String): String? =
    request.queryParameters.getAll(name)?.firstOrNull { it.isNotBlank() }
        ?: request.queryParameters[name]?.takeIf { it.isNotBlank() }

/**
 * Parses a strictly positive integer query parameter, or returns [default] when the parameter is absent.
 *
 * @throws ValidationException with [CommonError.InvalidParameterValue] when present but not a positive integer
 */
fun ApplicationCall.parsePositiveIntQuery(paramName: String, default: Int): Int {
    val raw = firstNonBlankQueryValue(paramName) ?: return default
    val n = raw.toIntOrNull() ?: throw ValidationException(CommonError.InvalidParameterValue(paramName))
    if (n < 1) throw ValidationException(CommonError.InvalidParameterValue(paramName))
    return n
}
