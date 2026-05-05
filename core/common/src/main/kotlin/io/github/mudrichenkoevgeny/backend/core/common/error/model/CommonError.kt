package io.github.mudrichenkoevgeny.backend.core.common.error.model

import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorCodes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

/**
 * Common application errors for request handling, validation, and infrastructure.
 *
 * Distinguishes [publicArgs] (exposed to the client and used in localized messages)
 * from [secretArgs] (for logs only). Each variant has a stable [code] for i18n and
 * a unique [errorId] for correlation.
 */
sealed class CommonError(
    override val errorId: ErrorId,
    override val code: String,
    override val publicArgs: Map<String, Any>? = null,
    override val secretArgs: Map<String, Any>? = null,
    override val httpStatusCode: HttpStatusCode,
    override val appErrorSeverity: AppErrorSeverity
) : AppError {

    /**
     * Unclassified or unexpected error.
     *
     * @param message Optional internal description; stored in [secretArgs], not sent to the client.
     */
    class Unknown(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.UNKNOWN,
        secretArgs = message?.let { message -> mapOf(CommonErrorArgs.MESSAGE to message) },
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    /**
     * Internal error caused by an unhandled exception.
     *
     * @param throwable The caught exception; its message is stored in [secretArgs].
     * @param call Optional request context for logging; not exposed to the client.
     */
    class Internal(
        val throwable: Throwable,
        val call: ApplicationCall? = null
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.INTERNAL,
        secretArgs = throwable.message?.let { message -> mapOf(CommonErrorArgs.MESSAGE to message) },
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    /**
     * Database or persistence layer error.
     *
     * @param message Optional internal description; stored in [secretArgs], not sent to the client.
     */
    class Database(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.INTERNAL,
        secretArgs = message?.let { message -> mapOf(CommonErrorArgs.MESSAGE to message) },
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    /**
     * Service or dependency temporarily unavailable.
     *
     * @param message Optional internal description; stored in [secretArgs], not sent to the client.
     */
    class ServiceUnavailable(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.SERVICE_UNAVAILABLE,
        secretArgs = message?.let { message -> mapOf(CommonErrorArgs.MESSAGE to message) },
        httpStatusCode = HttpStatusCode.ServiceUnavailable,
        appErrorSeverity = AppErrorSeverity.HIGH
    )

    /**
     * Generic "not found" error for domain objects (resource/entity missing).
     *
     * Useful when a specific feature-level error is not needed.
     *
     * @param resource Logical resource name.
     * @param identifier Optional identifier value used for lookup.
     */
    class NotFound(
        val resource: String,
        val identifier: String? = null
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.NOT_FOUND,
        publicArgs = mapOf(
            CommonErrorArgs.RESOURCE to resource
        ),
        secretArgs = identifier?.let { identifier -> mapOf(CommonErrorArgs.IDENTIFIER to identifier) },
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Rate limit exceeded for the given action.
     *
     * @param rateLimitActionCode Action that was rate-limited; in [secretArgs].
     * @param limit Allowed limit; in [secretArgs].
     * @param identifier Client or user identifier; in [secretArgs].
     * @param retryAfterSeconds Seconds until the client may retry; in [publicArgs], used in localized message.
     */
    class TooManyRequests(
        rateLimitActionCode: String,
        limit: Int,
        identifier: String,
        retryAfterSeconds: Int
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.TOO_MANY_REQUESTS,
        publicArgs = mapOf(
            CommonErrorArgs.RETRY_AFTER_SECONDS to retryAfterSeconds
        ),
        secretArgs = mapOf(
            CommonErrorArgs.RATE_LIMIT_ACTION_CODE to rateLimitActionCode,
            CommonErrorArgs.LIMIT to limit,
            CommonErrorArgs.IDENTIFIER to identifier
        ),
        httpStatusCode = HttpStatusCode.TooManyRequests,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * A required query/path parameter was omitted.
     *
     * @param parameterName Name of the missing parameter; in [publicArgs], used in localized message.
     */
    class MissingRequiredParameter(
        val parameterName: String
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.MISSING_REQUIRED_PARAMETER,
        publicArgs = mapOf(
            CommonErrorArgs.PARAMETER_NAME to parameterName
        ),
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * A query/path parameter has an invalid or unsupported value.
     *
     * @param parameterName Name of the invalid parameter; in [publicArgs], used in localized message.
     */
    class InvalidParameterValue(
        val parameterName: String
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.INVALID_PARAMETER_VALUE,
        publicArgs = mapOf(
            CommonErrorArgs.PARAMETER_NAME to parameterName
        ),
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * A required field is missing in the request body.
     *
     * @param fieldName Name of the missing field; in [publicArgs], used in localized message.
     */
    class MissingRequiredField(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.MISSING_REQUIRED_FIELD,
        publicArgs = mapOf(
            CommonErrorArgs.FIELD_NAME to fieldName
        ),
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * A string field is blank (null, empty, or only whitespace).
     *
     * @param fieldName Name of the field; in [publicArgs], used in localized message.
     */
    class BlankStringField(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.BLANK_STRING_FIELD,
        publicArgs = mapOf(
            CommonErrorArgs.FIELD_NAME to fieldName
        ),
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * A collection or array field is empty when at least one element is required.
     *
     * @param fieldName Name of the field; in [publicArgs], used in localized message.
     */
    class EmptyCollectionField(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.EMPTY_COLLECTION_FIELD,
        publicArgs = mapOf(
            CommonErrorArgs.FIELD_NAME to fieldName
        ),
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * A field value fails validation (format, range, or business rule).
     *
     * @param fieldName Name of the field; in [publicArgs], used in localized message.
     */
    class InvalidFieldValue(
        val fieldName: String
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.INVALID_FIELD_VALUE,
        publicArgs = mapOf(
            CommonErrorArgs.FIELD_NAME to fieldName
        ),
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Generic bad request (malformed or invalid in an unspecified way).
     *
     * @param message Optional internal description; stored in [secretArgs], not sent to the client.
     */
    class BadRequest(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.BAD_REQUEST,
        secretArgs = message?.let { message -> mapOf(CommonErrorArgs.MESSAGE to message) },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Request body is not valid JSON or does not match the expected schema.
     *
     * @param message Optional parse/schema error details; stored in [secretArgs], not sent to the client.
     */
    class InvalidJsonBody(
        val message: String? = null
    ) : CommonError(
        errorId = ErrorId.generate(),
        code = CommonErrorCodes.INVALID_JSON_BODY,
        secretArgs = message?.let { message -> mapOf(CommonErrorArgs.MESSAGE to message) },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )
}