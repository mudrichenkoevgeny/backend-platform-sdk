package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.mudrichenkoevgeny.backend.feature.user.network.contract.UserTokenClaims
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.installTestAuth() {
    install(Authentication) {
        jwt(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            val verifier = JWT
                .require(Algorithm.HMAC256("test-secret"))
                .build()

            verifier(verifier)

            validate { credential ->
                val testToken = JWT.create()
                    .withSubject(credential.payload.subject)
                    .withClaim(UserTokenClaims.USER_ROLE, UserRole.ADMIN.serialName)
                    .withClaim(UserTokenClaims.SESSION_ID, "00000000-0000-0000-0000-000000000001")
                    .sign(Algorithm.HMAC256("test-secret"))

                val decoded = JWT.decode(testToken)
                JWTPrincipal(decoded)
            }
        }
    }
}