package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.common.localization.CommonModuleStrings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback [AppErrorParser] implementation that uses `shared-foundation` [CommonModuleStrings].
 */
@Singleton
class CommonErrorParser @Inject constructor() : AppErrorParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse {
        val strings = CommonModuleStrings(locale = locale).errors
        val message = strings.unknown

        return ApiErrorResponse(
            id = errorId.asHexDashString(),
            code = code,
            message = message,
            args = args?.mapValues { it.value.toString() } ?: emptyMap()
        )
    }

    override fun getApiErrorResponse(appError: AppError, locale: String): ApiErrorResponse {
        return getApiErrorResponse(
            errorId = appError.errorId,
            code = appError.code,
            args = appError.publicArgs,
            locale = locale
        )
    }
}