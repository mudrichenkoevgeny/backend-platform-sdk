package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError

/**
 * Validates and transforms a string into type [T] using the provided [parser].
 * * @throws RequestHandlingException with [CommonError.InvalidFieldValue] if the [parser] returns `null`.
 */
inline fun <T> String.validateFieldValue(
    fieldName: String,
    parser: (String) -> T?
): T {
    return parser(this) ?: throw RequestHandlingException(
        CommonError.InvalidFieldValue(fieldName)
    )
}