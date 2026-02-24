package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

fun <T> AppResult<T?>.mapNotNullOrError(appError: AppError): AppResult<T> =
    when (this) {
        is AppResult.Success -> data?.let { AppResult.Success(it) } ?: AppResult.Error(appError)
        is AppResult.Error -> this
    }

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}

inline fun <T> AppResult<T>.onError(block: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) block(error)
    return this
}

inline fun <T, R> AppResult<T>.flatMapSuccess(transform: (T) -> AppResult<R>): AppResult<R> {
    return when (this) {
        is AppResult.Success -> transform(data)
        is AppResult.Error -> AppResult.Error(error)
    }
}