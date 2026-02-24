package io.github.mudrichenkoevgeny.backend.core.security.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorCodes
import io.ktor.http.HttpStatusCode
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
sealed class SecurityError(
    override val errorId: ErrorId,
    override val code: String,
    override val publicArgs: Map<String, Any>? = null,
    override val secretArgs: Map<String, Any>? = null,
    override val httpStatusCode: HttpStatusCode,
    override val appErrorSeverity: AppErrorSeverity
) : AppError {

    class AuthenticationConfirmationRequired() : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.AUTHENTICATION_CONFIRMATION_REQUIRED,
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    class PasswordTooWeak(publicArgs: Map<String, Any>) : SecurityError(
        errorId = ErrorId.generate(),
        code = SecurityErrorCodes.PASSWORD_TOO_WEAK,
        publicArgs = publicArgs,
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )
}