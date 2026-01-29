package io.github.mudrichenkoevgeny.backend.feature.user.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.feature.user.error.constants.UserErrorArgs
import io.github.mudrichenkoevgeny.backend.feature.user.error.constants.UserErrorCodes
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import java.util.UUID

sealed class UserError(
    override val errorId: ErrorId,
    override val call: ApplicationCall? = null,
    override val code: String,
    override val publicArgs: Map<String, Any>? = null,
    override val secretArgs: Map<String, Any>? = null,
    override val httpStatusCode: HttpStatusCode,
    override val appErrorSeverity: AppErrorSeverity
) : AppError {

    class InvalidAccessToken() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.INVALID_ACCESS_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class AccessTokenExpired() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.ACCESS_TOKEN_EXPIRED,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class InvalidRefreshToken() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.INVALID_REFRESH_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class InvalidSession() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.INVALID_SESSION,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class UserBlocked(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.USER_BLOCKED,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.value)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class UserReadOnly(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.USER_READ_ONLY,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.value)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class UserForbidden(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.USER_FORBIDDEN,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.value)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class UserNotFound(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.USER_NOT_FOUND,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.value)
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class InvalidCredentials() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.INVALID_CREDENTIALS,
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class CannotDeleteUserIdentifier() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.CAN_NOT_DELETE_USER_IDENTIFIER,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class CannotCreateUserIdentifier() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.CAN_NOT_CREATE_USER_IDENTIFIER,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class AlreadyHasUserIdentifierWithThatType() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.ALREADY_HAS_USER_IDENTIFIER_WITH_THAT_TYPE,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class AuthenticationConfirmationRequired() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.AUTHENTICATION_CONFIRMATION_REQUIRED,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class WrongPassword() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.WRONG_PASSWORD,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class PasswordTooWeak(publicArgs: Map<String, Any>) : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.PASSWORD_TOO_WEAK,
        publicArgs = publicArgs,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    class WrongConfirmationCode() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.WRONG_CONFIRMATION_CODE,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class ExternalIdMismatch() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.EXTERNAL_ID_MISMATCH,
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class ExternalTokenInvalid() : UserError(
        errorId = ErrorId(UUID.randomUUID()),
        code = UserErrorCodes.EXTERNAL_TOKEN_INVALID,
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )
}