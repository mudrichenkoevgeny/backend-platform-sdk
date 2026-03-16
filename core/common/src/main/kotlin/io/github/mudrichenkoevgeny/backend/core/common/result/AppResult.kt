package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

/**
 * A simple wrapper that represents the outcome of an application‑level operation.
 *
 * It intentionally mirrors the standard `Result<T>` type but is specialized
 * to carry domain‑specific [AppError] instances instead of generic `Throwable`s.
 */
sealed class AppResult<out T> {

    /**
     * Signals that an operation has completed successfully and produced [data].
     */
    data class Success<out T>(val data: T) : AppResult<T>()

    /**
     * Signals that an operation has failed with a domain‑specific [error].
     */
    data class Error(val error: AppError) : AppResult<Nothing>()

    /**
     * Exhaustively handles both branches of [AppResult].
     *
     * @param onSuccess callback invoked when the result is [Success]
     * @param onFailure callback invoked when the result is [Error]
     * @return the value returned by the executed callback
     */
    inline fun <R> fold(onSuccess: (T) -> R, onFailure: (AppError) -> R): R =
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onFailure(error)
        }
}