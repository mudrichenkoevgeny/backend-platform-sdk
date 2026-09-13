package io.github.mudrichenkoevgeny.backend.core.common.error.model

import io.ktor.http.HttpStatusCode

fun createTestAppError(
    code: String = "test_error",
    errorId: ErrorId = ErrorId.generate(),
    publicArgs: Map<String, Any>? = null,
    secretArgs: Map<String, Any>? = null,
    httpStatusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
    appErrorSeverity: AppErrorSeverity = AppErrorSeverity.MEDIUM
): AppError = object : AppError {
    override val errorId: ErrorId = errorId
    override val code: String = code
    override val publicArgs: Map<String, Any>? = publicArgs
    override val secretArgs: Map<String, Any>? = secretArgs
    override val httpStatusCode: HttpStatusCode = httpStatusCode
    override val appErrorSeverity: AppErrorSeverity = appErrorSeverity
}
