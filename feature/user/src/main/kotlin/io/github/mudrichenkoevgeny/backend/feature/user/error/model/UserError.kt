package io.github.mudrichenkoevgeny.backend.feature.user.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
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
     * Refresh token is missing or invalid.
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
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
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
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Account is under a security hold; access is restricted until resolved.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserSecurityHold(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_SECURITY_HOLD,
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Account is scheduled for deletion; most actions are not allowed.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserPendingDeletion(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_PENDING_DELETION,
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
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
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * User role does not allow performing this action.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserRoleNotAllowed(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_ROLE_NOT_ALLOWED,
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * User does not have required permissions to perform this action.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    class UserMissingPermissions(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_MISSING_PERMISSIONS,
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * User account status is incompatible with the requested operation.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging.
     */
    class UserIllegalAccountStatus(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_ILLEGAL_ACCOUNT_STATUS,
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * The operation is denied because the user's authority level is lower than required.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging.
     */
    class UserInsufficientAuthorityLevel(
        val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_INSUFFICIENT_AUTHORITY_LEVEL,
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
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
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
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
     * Maximum number of identifiers for a specific type or provider has been reached.
     *
     * @param maxNumberOfIdentifiers The limit allowed for the provider.
     * @param userAuthProvider The provider that reached its limit.
     */
    class UserIdentifierLimitReached(
        maxNumberOfIdentifiers: Int,
        userAuthProvider: UserAuthProvider
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_IDENTIFIER_LIMIT_REACHED,
        publicArgs = mapOf(
            UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS to maxNumberOfIdentifiers,
            UserErrorArgs.USER_AUTH_PROVIDER to userAuthProvider.serialName
        ),
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Overall maximum number of identifiers allowed for the account has been reached.
     *
     * @param maxNumberOfIdentifiers The total overall limit.
     */
    class TotalUserIdentifiersLimitReached(
        maxNumberOfIdentifiers: Int
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.TOTAL_USER_IDENTIFIERS_LIMIT_REACHED,
        publicArgs = mapOf(
            UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS to maxNumberOfIdentifiers
        ),
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
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
     * Linking with an external provider (OAuth/OpenID) failed.
     *
     * @param message Cause of the linkage failure; stored in [secretArgs] for logging.
     */
    class ExternalIdentifierLinkageFailed(
        message: String? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.EXTERNAL_IDENTIFIER_LINKAGE_FAILED,
        secretArgs = message?.let { mapOf(CommonErrorArgs.MESSAGE to it) },
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )
}