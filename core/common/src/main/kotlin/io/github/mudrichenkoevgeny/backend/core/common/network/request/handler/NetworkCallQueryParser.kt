package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall
import kotlin.time.Instant

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

/**
 * Retrieves all non-blank query string values for [name].
 */
fun ApplicationCall.getQueryValues(name: String): List<String> = request.queryParameters
    .getAll(name)
    ?.filter { it.isNotBlank() }
    .orEmpty()

/**
 * Parses query values for [paramName] and validates each element using [parseOrNull].
 *
 * @param paramName query parameter name
 * @param parseOrNull conversion function returning `null` when a raw string value is invalid
 * @return list of converted elements
 * @throws RequestHandlingException with [CommonError.InvalidParameterValue] when any element fails parsing
 */
inline fun <reified T> ApplicationCall.parseValidatedQueryList(
    paramName: String,
    crossinline parseOrNull: (String) -> T?
): List<T> = getQueryValues(paramName).map { raw ->
    parseOrNull(raw) ?: throw RequestHandlingException(CommonError.InvalidParameterValue(paramName))
}

/**
 * Parses a strict boolean query parameter, or returns `null` when absent.
 *
 * @param paramName query parameter name
 * @return parsed boolean value or `null`
 */
fun ApplicationCall.parseBooleanQueryOrNull(paramName: String): Boolean? =
    firstNonBlankQueryValue(paramName)?.toBooleanStrictOrNull()

/**
 * Parses an integer query parameter, or returns `null` when absent.
 *
 * @param paramName query parameter name
 * @return parsed integer value or `null`
 */
fun ApplicationCall.parseIntQueryOrNull(paramName: String): Int? =
    firstNonBlankQueryValue(paramName)?.toIntOrNull()

/**
 * Parses a long query parameter, or returns `null` when absent.
 *
 * @param paramName query parameter name
 * @return parsed long value or `null`
 */
fun ApplicationCall.parseLongQueryOrNull(paramName: String): Long? =
    firstNonBlankQueryValue(paramName)?.toLongOrNull()

/**
 * Parses an ISO-8601 timestamp string query parameter into a [Instant], or returns `null` when absent.
 *
 * @param paramName query parameter name
 * @return parsed [Instant] or `null`
 * @throws RequestHandlingException with [CommonError.InvalidParameterValue] when present but not a valid ISO-8601 timestamp
 */
fun ApplicationCall.parseTimestampQueryOrNull(paramName: String): Instant? {
    val raw = firstNonBlankQueryValue(paramName) ?: return null
    return try {
        Instant.parse(raw)
    } catch (_: Exception) {
        throw RequestHandlingException(CommonError.InvalidParameterValue(paramName))
    }
}
