package io.github.mudrichenkoevgeny.backend.feature.user.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.security.UserSecurityRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.session.SessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.configuration.UserConfigurationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.user.UserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry-point router for the user feature.
 *
 * Registers all user feature sub-routers and applies authentication requirements:
 * - Public endpoints (no JWT): auth flows and public configuration endpoints.
 * - Protected endpoints (JWT required): user profile, sessions, and security endpoints.
 *
 * Authentication is enforced via [JwtAuthSpecs].
 */
@Singleton
class UserFeatureRouter @Inject constructor(
    private val authRouter: AuthRouter,
    private val userRouter: UserRouter,
    private val sessionRouter: SessionRouter,
    private val userSecurityRouter: UserSecurityRouter,
    private val userConfigurationRouter: UserConfigurationRouter
) : BaseRouter {
    override fun register(route: Route) {
        authRouter.register(route)
        userConfigurationRouter.register(route)
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            userRouter.register(route)
            sessionRouter.register(route)
            userSecurityRouter.register(route)
        }
    }
}