package io.github.mudrichenkoevgeny.backend.core.common.error.model

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

/**
 * Represents an application error that can be propagated through the layers of the system.
 *
 * This interface distinguishes between information safe to expose to the client
 * (`publicArgs`) and sensitive/internal details (`secretArgs`), while keeping
 * a unique `errorId` for tracking and correlation in logs.
 */
interface AppError {
    val errorId: ErrorId
    val call: ApplicationCall? // todo do we need that on every error? log it?
    val code: String
    val publicArgs: Map<String, Any>?
    val secretArgs: Map<String, Any>?
    val httpStatusCode: HttpStatusCode
    val appErrorSeverity: AppErrorSeverity
}