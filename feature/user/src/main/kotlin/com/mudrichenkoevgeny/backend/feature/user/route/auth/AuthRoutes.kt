package io.github.mudrichenkoevgeny.backend.feature.user.route.auth

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.login.LoginRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.refreshtoken.RefreshTokenRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.register.RegisterRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.resetpassword.ResetPasswordRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

object AuthRoutes {
    const val BASE_AUTH_ROUTE = "/auth"
}

@Singleton
class AuthRouter @Inject constructor(
    private val refreshTokenRouter: RefreshTokenRouter,
    private val loginRouter: LoginRouter,
    private val registerRouter: RegisterRouter,
    private val resetPasswordRouter: ResetPasswordRouter
) : BaseRouter {
    override fun register(route: Route) {
        refreshTokenRouter.register(route)
        loginRouter.register(route)
        registerRouter.register(route)
        resetPasswordRouter.register(route)
    }
}