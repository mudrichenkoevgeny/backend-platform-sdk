package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

/**
 * Converts an [AppResult] that may contain `null` data into a non‑nullable [AppResult].
 *
 * - If this is [AppResult.Success] and `data != null`, wraps the non‑null value in [AppResult.Success].
 * - If this is [AppResult.Success] and `data == null`, returns [AppResult.Error] with the provided [appError].
 * - If this is [AppResult.Error], returns the original error unchanged.
 */
fun <T> AppResult<T?>.mapNotNullOrError(appError: AppError): AppResult<T> =
    when (this) {
        is AppResult.Success -> data?.let { AppResult.Success(it) } ?: AppResult.Error(appError)
        is AppResult.Error -> this
    }

/**
 * Executes [block] if the receiver is [AppResult.Success] and returns the original result unchanged.
 *
 * Useful for side‑effects such as logging without breaking fluent chains.
 */
inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}

/**
 * Executes [block] if the receiver is [AppResult.Error] and returns the original result unchanged.
 *
 * Useful for centralized error logging or metrics collection.
 */
inline fun <T> AppResult<T>.onError(block: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) block(error)
    return this
}

/**
 * Monadic bind/flatMap operation for [AppResult] that only transforms successful values.
 *
 * - If this is [AppResult.Success], applies [transform] and returns its result.
 * - If this is [AppResult.Error], propagates the existing error.
 */
inline fun <T, R> AppResult<T>.flatMapSuccess(transform: (T) -> AppResult<R>): AppResult<R> =
    when (this) {
        is AppResult.Success -> transform(data)
        is AppResult.Error -> AppResult.Error(error)
    }

/**
 * Extracts successful data or returns `null` for errors.
 *
 * - If this is [AppResult.Success], returns [AppResult.Success.data].
 * - If this is [AppResult.Error], returns `null`.
 */
fun <T> AppResult<T>.dataOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Error -> null
}