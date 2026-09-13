package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves application errors using a chain of responsibility pattern.
 *
 * Iterates through [specificParsers] to find the first parser that can handle the error.
 * If no specific parser returns a result, delegates to [commonParser].
 */
@Singleton
class AppErrorParserResolver @Inject constructor(
    private val commonParser: CommonErrorParser,
    private val specificParsers: Set<@JvmSuppressWildcards AppErrorSpecificParser>
) : AppErrorParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse {
        for (parser in specificParsers) {
            val result = parser.getApiErrorResponse(errorId, code, args, locale)
            if (result != null) return result
        }
        return commonParser.getApiErrorResponse(errorId, code, args, locale)
    }

    override fun getApiErrorResponse(
        appError: AppError,
        locale: String
    ): ApiErrorResponse {
        for (parser in specificParsers) {
            val result = parser.getApiErrorResponse(appError, locale)
            if (result != null) return result
        }
        return commonParser.getApiErrorResponse(appError, locale)
    }
}
