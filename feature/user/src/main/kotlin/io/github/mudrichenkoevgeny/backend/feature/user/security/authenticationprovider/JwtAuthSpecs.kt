package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

/**
 * Named authentication configurations used by the user feature.
 *
 * The name is referenced from Ktor `authenticate(...)` blocks.
 */
object JwtAuthSpecs {
    const val AUTHENTICATE_CONFIGURATION = "jwt"
}