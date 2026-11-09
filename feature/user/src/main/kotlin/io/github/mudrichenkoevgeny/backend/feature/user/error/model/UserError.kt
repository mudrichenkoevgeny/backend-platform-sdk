package io.github.mudrichenkoevgeny.backend.feature.user.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.ktor.http.HttpStatusCode
import kotlin.time.Instant

/**
 * User and authentication feature errors: tokens, sessions, credentials, and account state.
 *
 * Uses [publicArgs] for client-visible data; [secretArgs] (e.g. [UserId]) for logs only.
 * Each variant has a stable [code] for i18n and a unique [errorId] for correlation.
 */
public sealed class UserError(
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
    public class InvalidAccessToken : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_ACCESS_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Access token has expired; client should refresh or re-authenticate.
     */
    public class AccessTokenExpired : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.ACCESS_TOKEN_EXPIRED,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Refresh token is missing or invalid.
     */
    public class InvalidRefreshToken : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_REFRESH_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Session is invalid or no longer exists (e.g. logged out elsewhere).
     */
    public class InvalidSession : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_SESSION,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * User account has been banned.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    public class UserBanned(
        public val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_BANNED,
        secretArgs = userId?.let { userId ->
            mapOf(UserErrorArgs.USER_ID to userId.asHexDashString())
        },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * User account is temporarily or indefinitely locked out due to security checks or failed login attempts.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     * @param lockoutType Category of account lockout; sent in [publicArgs].
     * @param temporaryLockoutUntil Optional timestamp when temporary lockout expires; sent in [publicArgs].
     */
    public class UserLocked(
        public val userId: UserId? = null,
        public val lockoutType: AccountLockoutType,
        public val temporaryLockoutUntil: Instant? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_LOCKED,
        secretArgs = userId?.let { userId ->
            mapOf(UserErrorArgs.USER_ID to userId.asHexDashString())
        },
        publicArgs = buildMap {
            put(
                UserErrorArgs.ACCOUNT_LOCKOUT_TYPE,
                lockoutType.value
            )
            temporaryLockoutUntil?.let {
                put(
                    UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL,
                    it.toEpochMilliseconds().toString()
                )
            }
        },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * User account is in read-only mode; write operations are forbidden.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging, not sent to the client.
     */
    public class UserReadOnly(
        public val userId: UserId? = null
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
    public class UserSecurityHold(
        public val userId: UserId? = null
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
    public class UserPendingDeletion(
        public val userId: UserId? = null
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
    public class UserForbidden(
        public val userId: UserId? = null
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
    public class UserRoleNotAllowed(
        public val userId: UserId? = null
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
    public class UserMissingPermissions(
        public val userId: UserId? = null
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
    public class UserIllegalAccountStatus(
        public val userId: UserId? = null
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
    public class UserInsufficientAuthorityLevel(
        public val userId: UserId? = null
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
    public class UserNotFound(
        public val userId: UserId? = null
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
    public class InvalidCredentials : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_CREDENTIALS,
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Cannot remove this sign-in method (e.g. last identifier, or policy forbids it).
     */
    public class CannotDeleteUserIdentifier : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.CAN_NOT_DELETE_USER_IDENTIFIER,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Cannot add this sign-in method (e.g. already linked elsewhere, or provider error).
     */
    public class CannotCreateUserIdentifier : UserError(
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
    public class UserIdentifierLimitReached(
        public val maxNumberOfIdentifiers: Int,
        public val userAuthProvider: UserAuthProvider
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_IDENTIFIER_LIMIT_REACHED,
        publicArgs = mapOf(
            UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS to maxNumberOfIdentifiers,
            UserErrorArgs.USER_AUTH_PROVIDER to userAuthProvider.value
        ),
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Overall maximum number of identifiers allowed for the account has been reached.
     *
     * @param maxNumberOfIdentifiers The total overall limit.
     */
    public class TotalUserIdentifiersLimitReached(
        public val maxNumberOfIdentifiers: Int
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
    public class WrongPassword : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.WRONG_PASSWORD,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Email/phone verification or reset code is wrong or expired.
     */
    public class WrongConfirmationCode : UserError(
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
    public class ExternalIdentifierLinkageFailed(
        public val message: String? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.EXTERNAL_IDENTIFIER_LINKAGE_FAILED,
        secretArgs = message?.let { mapOf(CommonErrorArgs.MESSAGE to it) },
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * User registration is currently disabled.
     */
    public class RegistrationDisabled : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.REGISTRATION_DISABLED,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Self-service account unlock is currently disabled by security policy.
     */
    public class SelfServiceUnlockDisabled : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.SELF_SERVICE_UNLOCK_DISABLED,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * The registration, login, or email linkage attempt was rejected because the email address or domain is not permitted.
     */
    public class EmailNotAllowed : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.EMAIL_NOT_ALLOWED,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Authentication or operation failed because the account has no password set or initial password setup is required.
     *
     * @param userId Optional user id; stored in [secretArgs] for logging.
     */
    public class PasswordSetupRequired(
        public val userId: UserId? = null
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.PASSWORD_SETUP_REQUIRED,
        secretArgs = userId?.let { userId -> mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()) },
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Operation failed because the specified identifier type does not support password credentials.
     */
    public class UserIdentifierPasswordNotSupported : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_IDENTIFIER_PASSWORD_NOT_SUPPORTED,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Operation failed because no password credential is set on the specified identifier record.
     */
    public class UserIdentifierPasswordNotSet : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.USER_IDENTIFIER_PASSWORD_NOT_SET,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The requested nickname is already taken by another user.
     */
    public class NicknameAlreadyTaken(
        public val nickname: String
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.NICKNAME_ALREADY_TAKEN,
        publicArgs = mapOf(UserErrorArgs.NICKNAME to nickname),
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Modifying the nickname is disabled by system configuration.
     */
    public class NicknameChangeDisabled : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.NICKNAME_CHANGE_DISABLED,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided nickname character length is below the required minimum.
     */
    public class NicknameTooShort(
        public val minLength: Int
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.NICKNAME_TOO_SHORT,
        publicArgs = mapOf(UserErrorArgs.NICKNAME_MIN_LENGTH to minLength),
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided nickname character length exceeds the allowed maximum.
     */
    public class NicknameTooLong(
        public val maxLength: Int
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.NICKNAME_TOO_LONG,
        publicArgs = mapOf(UserErrorArgs.NICKNAME_MAX_LENGTH to maxLength),
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided nickname contains unsupported or illegal characters.
     */
    public class InvalidNicknameCharacters : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_NICKNAME_CHARACTERS,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided display name character length is below the required minimum.
     */
    public class DisplayNameTooShort(
        public val minLength: Int
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.DISPLAY_NAME_TOO_SHORT,
        publicArgs = mapOf(UserErrorArgs.DISPLAY_NAME_MIN_LENGTH to minLength),
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided display name character length exceeds the allowed maximum.
     */
    public class DisplayNameTooLong(
        public val maxLength: Int
    ) : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.DISPLAY_NAME_TOO_LONG,
        publicArgs = mapOf(UserErrorArgs.DISPLAY_NAME_MAX_LENGTH to maxLength),
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided display name contains illegal control characters or line breaks.
     */
    public class InvalidDisplayNameCharacters : UserError(
        errorId = ErrorId.generate(),
        code = UserErrorCodes.INVALID_DISPLAY_NAME_CHARACTERS,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )
}
