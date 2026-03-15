package io.github.mudrichenkoevgeny.backend.feature.user.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.ktor.http.HttpStatusCode

/**
 * User and authentication feature errors: tokens, sessions, credentials, and account state.
 *
 * Uses [publicArgs] for client-visible data; [secretArgs] (e.g. [UserId]) for logs only.
 * Each variant has a stable [code] for i18n and a unique [errorId] for correlation.
 */
sealed class UserError(
    override val errorId: ErrorId,
    override val code: String,
    override val publicArgs: Map<String, Any>? = null,
    override val secretArgs: Map<String, Any>? = null,
    override val httpStatusCode: HttpStatusCode,
    override val appErrorSeverity: AppErrorSeverity
) : AppError {

    /**
     * Access token is missing, malformed, or signature invalid.
     */
    class InvalidAccessToken() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_ACCESS_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Access token has expired; client should refresh or re-authenticate.
     */
    class AccessTokenExpired() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.ACCESS_TOKEN_EXPIRED,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Refresh token is missing, invalid, or revoked.
     */
    class InvalidRefreshToken() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_REFRESH_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Session is invalid or no longer exists (e.g. logged out elsewhere).
     */
    class InvalidSession() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_SESSION,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * User account has been blocked and cannot perform the action.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserBlocked(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_BLOCKED,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.asHexDashString())
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * User account is in read-only mode; write operations are forbidden.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserReadOnly(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_READ_ONLY,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.asHexDashString())
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * User is not allowed to access this resource (insufficient rights or context).
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserForbidden(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_FORBIDDEN,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.asHexDashString())
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * No user exists for the given identifier or context.
     *
     * @param userId Optional user id that was looked up; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserNotFound(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_NOT_FOUND,
        secretArgs = buildMap {
            if (userId != null) {
                put(UserErrorArgs.USER_ID, userId.asHexDashString())
            }
        }.takeIf { it.isNotEmpty() },
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Login failed: email or password incorrect (or account not found for email).
     */
    class InvalidCredentials() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_CREDENTIALS,
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Cannot remove this sign-in method (e.g. last identifier, or policy forbids it).
     */
    class CannotDeleteUserIdentifier() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.CAN_NOT_DELETE_USER_IDENTIFIER,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Cannot add this sign-in method (e.g. already linked elsewhere, or provider error).
     */
    class CannotCreateUserIdentifier() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.CAN_NOT_CREATE_USER_IDENTIFIER,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * User already has a sign-in method of this type (e.g. email or same OAuth provider).
     */
    class AlreadyHasUserIdentifierWithThatType() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.ALREADY_HAS_USER_IDENTIFIER_WITH_THAT_TYPE,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Current password is incorrect (e.g. when changing password).
     */
    class WrongPassword() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.WRONG_PASSWORD,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Email/phone verification or reset code is wrong or expired.
     */
    class WrongConfirmationCode() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.WRONG_CONFIRMATION_CODE,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * External provider id does not match or could not be linked (e.g. already linked to another account).
     *
     * @param throwable Optional cause; stored in [secretArgs] for logging, not sent to the client.
     */
    class ExternalIdMismatch(
        val throwable: Throwable? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.EXTERNAL_ID_MISMATCH,
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * External token is invalid, expired, or revoked.
     *
     */
    class ExternalTokenInvalid() : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.EXTERNAL_TOKEN_INVALID,
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )
}