package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError

/**
 * Internal validation outcome used by Ktor JWT authentication.
 *
 * This type is stored as an authentication principal when validation fails and is later
 * mapped to an [AppError] in the authentication challenge.
 */
public sealed class JwtValidationError {
    /** Token is missing, malformed, or signature invalid. */
    public object InvalidToken : JwtValidationError()

    /** Token is well-formed but expired. */
    public object TokenExpired : JwtValidationError()

    /** Token is valid, but the referenced user does not exist. */
    public object UserNotFound : JwtValidationError()
}

/**
 * Maps a JWT validation failure to a public [AppError] suitable for HTTP responses.
 */
public fun JwtValidationError.toAppError(): AppError = when (this) {
    JwtValidationError.InvalidToken -> UserError.InvalidAccessToken()
    JwtValidationError.TokenExpired -> UserError.AccessTokenExpired()
    JwtValidationError.UserNotFound -> UserError.UserNotFound()
}
