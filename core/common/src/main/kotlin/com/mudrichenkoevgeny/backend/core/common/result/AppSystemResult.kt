package com.mudrichenkoevgeny.backend.core.common.result

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError

sealed class AppSystemResult<out T> {
    data class Success<out T>(val data: T) : AppSystemResult<T>()
    data class Error(val systemError: CommonError.System) : AppSystemResult<Nothing>()
}