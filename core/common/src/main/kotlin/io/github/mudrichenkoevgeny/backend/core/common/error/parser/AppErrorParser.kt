package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse

const val DEFAULT_LOCALE = "en"
const val UNKNOWN_ERROR_MESSAGE = "Unknown error"

/**
 * Provides localized messages for application errors (AppError).
 *
 * This class loads JSON resource files containing error messages for multiple locales,
 * caches them, and allows retrieving messages by error code and locale, optionally
 * formatting them with arguments.
 */
interface AppErrorParser {
    fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>? = null,
        locale: String = DEFAULT_LOCALE
    ): ApiErrorResponse

    fun getApiErrorResponse(
        appError: AppError,
        locale: String = DEFAULT_LOCALE
    ): ApiErrorResponse
}