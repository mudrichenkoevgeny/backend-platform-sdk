package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature-specific error parser for common-related errors (e.g., validation, internal errors).
 * Currently serves as a template for potential future common-specific parsing.
 */
@Singleton
class CommonAppErrorSpecificParser @Inject constructor() : AppErrorSpecificParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String,
    ): ApiErrorResponse? {
        return null
    }

    override fun getApiErrorResponse(
        appError: AppError,
        locale: String,
    ): ApiErrorResponse? {
        return null
    }
}
