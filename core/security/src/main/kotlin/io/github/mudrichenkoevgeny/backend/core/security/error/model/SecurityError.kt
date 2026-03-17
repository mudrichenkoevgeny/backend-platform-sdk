package io.github.mudrichenkoevgeny.backend.core.security.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorCodes
import io.ktor.http.HttpStatusCode

/**
 * Security-related errors: authentication requirements and password policy.
 *
 * Uses `publicArgs` for client-visible data (e.g. password rule details in [PasswordTooWeak]) and
 * `secretArgs` for internal-only data. Each variant has a stable `code` for i18n and a unique
 * `errorId` for correlation.
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
     * User must complete an additional authentication step (e.g. 2FA, email confirmation).
     */
    class AuthenticationConfirmationRequired : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.AUTHENTICATION_CONFIRMATION_REQUIRED,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Password does not satisfy security policy (length, complexity, etc.).
     *
     * @param publicArgs Map of rule names to boolean/string values (e.g. passwordTooShort,
     * passwordMinLength). The map is stored in `publicArgs`, exposed to the client, and used by
     * localized error messages.
     */
    class PasswordTooWeak(publicArgs: Map<String, Any>) : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.PASSWORD_TOO_WEAK,
        publicArgs = publicArgs,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )
}