package com.mudrichenkoevgeny.backend.core.common.error.model

import com.mudrichenkoevgeny.backend.core.common.error.constants.CommonErrorArgs
import com.mudrichenkoevgeny.backend.core.common.error.constants.CommonErrorCodes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import java.util.UUID

sealed class CommonError(
    override val errorId: ErrorId,
    override val call: ApplicationCall? = null,
    override val code: String,
    override val publicArgs: Map<String, Any>? = null,
    override val secretArgs: Map<String, Any>? = null,
    override val httpStatusCode: HttpStatusCode,
    override val appErrorSeverity: AppErrorSeverity
) : AppError {

    class System(
        val throwable: Throwable,
        call: ApplicationCall? = null
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        call = call,
        code = CommonErrorCodes.THROWABLE,
        secretArgs = throwable.message
            ?.let { message -> mapOf(CommonErrorArgs.MESSAGE to message) },
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    class Unknown(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.UNKNOWN,
        secretArgs = buildMap {
            if (message != null) {
                put(CommonErrorArgs.MESSAGE, message)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    class Database(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.DATABASE,
        secretArgs = buildMap {
            if (message != null) {
                put(CommonErrorArgs.MESSAGE, message)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    class Redis(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.REDIS,
        secretArgs = buildMap {
            if (message != null) {
                put(CommonErrorArgs.MESSAGE, message)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    class ServiceUnavailable(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.SERVICE_UNAVAILABLE,
        secretArgs = buildMap {
            if (message != null) {
                put(CommonErrorArgs.MESSAGE, message)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.ServiceUnavailable,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    class TooManyRequests(
        rateLimitActionCode: String,
        limit: Int,
        identifier: String,
        retryAfterSeconds: Int,
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.TOO_MANY_REQUESTS,
        publicArgs = buildMap {
            put(CommonErrorArgs.RETRY_AFTER_SECONDS, retryAfterSeconds)
        },
        secretArgs = buildMap {
            put(CommonErrorArgs.RATE_LIMIT_ACTION_CODE, rateLimitActionCode)
            put(CommonErrorArgs.LIMIT, limit)
            put(CommonErrorArgs.IDENTIFIER, identifier)
        },
        httpStatusCode = HttpStatusCode.TooManyRequests,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class MissingRequiredParameter(
        val parameterName: String
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.MISSING_REQUIRED_PARAMETER,
        publicArgs = buildMap {
            put(CommonErrorArgs.PARAMETER_NAME, parameterName)
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class InvalidParameterValue(
        val parameterName: String
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.INVALID_PARAMETER_VALUE,
        publicArgs = buildMap {
            put(CommonErrorArgs.PARAMETER_NAME, parameterName)
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class MissingRequiredField(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.MISSING_REQUIRED_FIELD,
        publicArgs = buildMap {
            put(CommonErrorArgs.FIELD_NAME, fieldName)
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class BlankStringField(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.BLANK_STRING_FIELD,
        publicArgs = buildMap {
            put(CommonErrorArgs.FIELD_NAME, fieldName)
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class EmptyCollectionField(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.EMPTY_COLLECTION_FIELD,
        publicArgs = buildMap {
            put(CommonErrorArgs.FIELD_NAME, fieldName)
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class InvalidFieldValue(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.INVALID_FIELD_VALUE,
        publicArgs = buildMap {
            put(CommonErrorArgs.FIELD_NAME, fieldName)
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class BadRequest(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.BAD_REQUEST,
        secretArgs = buildMap {
            if (message != null) {
                put(CommonErrorArgs.MESSAGE, message)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class InvalidJsonBody(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId(UUID.randomUUID()),
        code = CommonErrorCodes.INVALID_JSON_BODY,
        secretArgs = buildMap {
            if (message != null) {
                put(CommonErrorArgs.MESSAGE, message)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )
}