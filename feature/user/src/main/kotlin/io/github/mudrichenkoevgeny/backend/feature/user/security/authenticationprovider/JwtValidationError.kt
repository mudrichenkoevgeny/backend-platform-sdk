package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError

sealed class JwtValidationError {
    object InvalidToken : JwtValidationError()
    object TokenExpired : JwtValidationError()
    object UserNotFound : JwtValidationError()
}

fun JwtValidationError.toAppError(): AppError = when (this) {
    JwtValidationError.InvalidToken -> UserError.InvalidAccessToken()
    JwtValidationError.TokenExpired -> UserError.AccessTokenExpired()
    JwtValidationError.UserNotFound -> UserError.UserNotFound()
}