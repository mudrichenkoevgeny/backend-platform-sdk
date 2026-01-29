package io.github.mudrichenkoevgeny.backend.feature.user.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.security.SecurityRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.session.SessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.user.UserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthenticationConstants
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserFeatureRoutes @Inject constructor(
    private val authRouter: AuthRouter,
    private val userRouter: UserRouter,
    private val sessionRouter: SessionRouter,
    private val securityRouter: SecurityRouter
) : BaseRouter {
    override fun register(route: Route) {
        authRouter.register(route)
        route.authenticate(JwtAuthenticationConstants.AUTHENTICATE_CONFIGURATION) {
            userRouter.register(route)
            sessionRouter.register(route)
            securityRouter.register(route)
        }
    }
}