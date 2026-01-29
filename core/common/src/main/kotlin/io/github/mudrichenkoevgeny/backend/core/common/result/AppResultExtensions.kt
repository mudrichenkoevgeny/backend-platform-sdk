package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

fun <T> AppResult<T?>.mapNotNullOrError(appError: AppError): AppResult<T> =
    when (this) {
        is AppResult.Success -> data?.let { AppResult.Success(it) } ?: AppResult.Error(appError)
        is AppResult.Error -> this
    }