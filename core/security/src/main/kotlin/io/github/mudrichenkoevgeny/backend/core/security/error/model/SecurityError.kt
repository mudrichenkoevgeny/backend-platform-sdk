package io.github.mudrichenkoevgeny.backend.core.security.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorCodes
import io.ktor.http.HttpStatusCode

/**
 * Security-related errors: authentication requirements and password policy.
 */
sealed class SecurityError(
    override val errorId: ErrorId,
    override val code: String,
    override val publicArgs: Map<String, Any>? = null,
    override val secretArgs: Map<String, Any>? = null,
    override val httpStatusCode: HttpStatusCode,
    override val appErrorSeverity: AppErrorSeverity
) : AppError {

    /**
     * Action requires confirmation via a TOTP (Time-based One-Time Password).
     *
     * @param mfaToken The token required for the subsequent verification request.
     */
    class TotpConfirmationRequired(
        mfaToken: String
    ) : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.TOTP_CONFIRMATION_REQUIRED,
        publicArgs = mapOf(
            SecurityErrorArgs.MFA_TOKEN to mfaToken
        ),
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Password does not satisfy security policy (length, complexity, etc.).
     *
     * @param publicArgs Map of rule names to boolean/string values (e.g. passwordTooShort,
     * passwordMinLength) used for localized error messages.
     */
    class PasswordTooWeak(
        publicArgs: Map<String, Any>
    ) : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.PASSWORD_TOO_WEAK,
        publicArgs = publicArgs,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * A new OTP request was made before the cooling-off period had elapsed.
     *
     * @param retryAfterSeconds Remaining wait time in seconds.
     */
    class OtpRetryTooSoon(
        retryAfterSeconds: Int
    ) : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.OTP_RETRY_TOO_SOON,
        publicArgs = mapOf(
            CommonErrorArgs.RETRY_AFTER_SECONDS to retryAfterSeconds
        ),
        httpStatusCode = HttpStatusCode.TooManyRequests,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Indicates that TOTP is already configured and active for the account.
     */
    class TotpAlreadyEnabled : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.TOTP_ALREADY_ENABLED,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Operation failed because TOTP has not been set up for this account.
     */
    class TotpNotEnabled : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.TOTP_NOT_ENABLED,
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided MFA challenge token has expired and cannot be used for verification.
     */
    class MfaTokenExpired : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.MFA_TOKEN_EXPIRED,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The recovery code has already been used and is no longer valid for authentication.
     */
    class RecoveryCodeAlreadyUsed : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.RECOVERY_CODE_ALREADY_USED,
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The TOTP or recovery code provided by the user is incorrect.
     */
    class InvalidTotpCode : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.INVALID_TOTP_CODE,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * The provided MFA challenge token is invalid, malformed, or belongs to another session.
     */
    class InvalidMfaToken : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.INVALID_MFA_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )
}