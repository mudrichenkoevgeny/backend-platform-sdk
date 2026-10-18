package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.localization.CommonModuleStrings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature-specific error parser for common-related errors (validation, internal, rate limiting, etc.).
 */
@Singleton
class CommonAppErrorSpecificParser @Inject constructor() : AppErrorSpecificParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse? {
        val strings = CommonModuleStrings(locale = locale).errors

        val message = when (code) {
            CommonErrorCodes.UNKNOWN -> strings.unknown
            CommonErrorCodes.INTERNAL -> strings.internal
            CommonErrorCodes.BAD_REQUEST -> strings.badRequest
            CommonErrorCodes.INVALID_JSON_BODY -> strings.invalidJsonBody
            CommonErrorCodes.SERVICE_UNAVAILABLE -> strings.serviceUnavailable

            CommonErrorCodes.NOT_FOUND -> {
                val resource = args?.get(CommonErrorArgs.RESOURCE)
                if (resource != null) {
                    strings.notFound(resource)
                } else {
                    strings.notFoundWithoutArgs
                }
            }

            CommonErrorCodes.TOO_MANY_REQUESTS -> {
                val retryAfterSeconds = args?.get(CommonErrorArgs.RETRY_AFTER_SECONDS)
                if (retryAfterSeconds != null) {
                    strings.tooManyRequests(retryAfterSeconds)
                } else {
                    strings.tooManyRequestsWithoutArgs
                }
            }

            CommonErrorCodes.MISSING_REQUIRED_PARAMETER -> {
                val parameterName = args?.get(CommonErrorArgs.PARAMETER_NAME)
                if (parameterName != null) {
                    strings.missingParameter(parameterName)
                } else {
                    strings.missingParameterWithoutArgs
                }
            }

            CommonErrorCodes.INVALID_PARAMETER_VALUE -> {
                val parameterName = args?.get(CommonErrorArgs.PARAMETER_NAME)
                if (parameterName != null) {
                    strings.invalidParameter(parameterName)
                } else {
                    strings.invalidParameterWithoutArgs
                }
            }

            CommonErrorCodes.MISSING_REQUIRED_FIELD -> {
                val fieldName = args?.get(CommonErrorArgs.FIELD_NAME)
                if (fieldName != null) {
                    strings.missingField(fieldName)
                } else {
                    strings.missingFieldWithoutArgs
                }
            }

            CommonErrorCodes.BLANK_STRING_FIELD -> {
                val fieldName = args?.get(CommonErrorArgs.FIELD_NAME)
                if (fieldName != null) {
                    strings.blankField(fieldName)
                } else {
                    strings.blankFieldWithoutArgs
                }
            }

            CommonErrorCodes.EMPTY_COLLECTION_FIELD -> {
                val fieldName = args?.get(CommonErrorArgs.FIELD_NAME)
                if (fieldName != null) {
                    strings.emptyCollectionField(fieldName)
                } else {
                    strings.emptyCollectionFieldWithoutArgs
                }
            }

            CommonErrorCodes.INVALID_FIELD_VALUE -> {
                val fieldName = args?.get(CommonErrorArgs.FIELD_NAME)
                if (fieldName != null) {
                    strings.invalidField(fieldName)
                } else {
                    strings.invalidFieldWithoutArgs
                }
            }

            else -> return null
        }

        return ApiErrorResponse(
            id = errorId.asHexDashString(),
            code = code,
            message = message,
            args = args?.mapValues { it.value.toString() } ?: emptyMap()
        )
    }

    override fun getApiErrorResponse(
        appError: AppError,
        locale: String
    ): ApiErrorResponse? {
        return getApiErrorResponse(
            errorId = appError.errorId,
            code = appError.code,
            args = appError.publicArgs,
            locale = locale
        )
    }
}