package io.github.mudrichenkoevgeny.backend.feature.user.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.ManagementCoreUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.OpenCoreUserRouter
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
class UserRouter @Inject constructor(
    private val openCoreUserRouter: OpenCoreUserRouter,
    private val managementCoreUserRouter: ManagementCoreUserRouter
) : BaseRouter {
    override fun register(route: Route) {
        openCoreUserRouter.register(route)
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            managementCoreUserRouter.register(route)
        }
    }
}