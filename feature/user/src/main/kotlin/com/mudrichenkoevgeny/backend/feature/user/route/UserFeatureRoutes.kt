package com.mudrichenkoevgeny.backend.feature.user.route

import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRouter
import com.mudrichenkoevgeny.backend.feature.user.route.confirmation.ConfirmationRouter
import com.mudrichenkoevgeny.backend.feature.user.route.security.SecurityRouter
import com.mudrichenkoevgeny.backend.feature.user.route.session.SessionRouter
import com.mudrichenkoevgeny.backend.feature.user.route.user.UserRouter
import com.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthenticationConstants
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserFeatureRoutes @Inject constructor(
    private val authRouter: AuthRouter,
    private val userRouter: UserRouter,
    private val sessionRouter: SessionRouter,
    private val securityRouter: SecurityRouter,
    private val confirmationRouter: ConfirmationRouter,
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