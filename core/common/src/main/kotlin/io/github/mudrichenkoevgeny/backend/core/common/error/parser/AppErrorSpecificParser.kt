package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse

/**
 * Interface for feature-specific error parsers (nodes in the chain of responsibility).
 *
 * Implementations process specific error codes and return a localized [ApiErrorResponse],
 * or `null` if the error code is not handled by this parser.
 */
interface AppErrorSpecificParser {

    /**
     * Attempts to build an [ApiErrorResponse] for a specific error code and arguments.
     *
     * @return [ApiErrorResponse] if handled, or `null` to delegate to the next parser.
     */
    fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String,
    ): ApiErrorResponse?

    /**
     * Attempts to build an [ApiErrorResponse] from an [AppError].
     *
     * @return [ApiErrorResponse] if handled, or `null` to delegate to the next parser.
     */
    fun getApiErrorResponse(
        appError: AppError,
        locale: String,
    ): ApiErrorResponse?
}
