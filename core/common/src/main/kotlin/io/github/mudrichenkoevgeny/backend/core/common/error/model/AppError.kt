package io.github.mudrichenkoevgeny.backend.core.common.error.model

import io.ktor.http.HttpStatusCode

/**
 * Represents an application error that can be propagated through the layers of the system.
 *
 * This interface distinguishes between information safe to expose to the client
 * (`publicArgs`) and sensitive/internal details (`secretArgs`), while keeping
 * a unique `errorId` for tracking and correlation in logs.
 */
public interface AppError {
    public val errorId: ErrorId
    public val code: String
    public val publicArgs: Map<String, Any>?
    public val secretArgs: Map<String, Any>?
    public val httpStatusCode: HttpStatusCode
    public val appErrorSeverity: AppErrorSeverity
}
