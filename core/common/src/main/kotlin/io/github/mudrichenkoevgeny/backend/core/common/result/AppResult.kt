package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()

    inline fun <R> fold(onSuccess: (T) -> R, onFailure: (AppError) -> R): R =
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onFailure(error)
        }
}