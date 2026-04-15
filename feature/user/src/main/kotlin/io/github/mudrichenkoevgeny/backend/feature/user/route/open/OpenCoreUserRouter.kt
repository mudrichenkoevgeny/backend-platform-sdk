package io.github.mudrichenkoevgeny.backend.feature.user.route.open

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.AuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenCoreUserRouter @Inject constructor(
    private val authRouter: AuthRouter,

) : BaseRouter {
    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            managementAuthRouter.register(route)
            managementIdentifierRouter.register(route)
        }
    }
}