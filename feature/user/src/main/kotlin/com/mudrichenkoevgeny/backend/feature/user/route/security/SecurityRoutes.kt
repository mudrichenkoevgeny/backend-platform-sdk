package com.mudrichenkoevgeny.backend.feature.user.route.security

import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.feature.user.route.security.password.PasswordRouter
import com.mudrichenkoevgeny.backend.feature.user.route.security.useridentifiers.SecurityUserIdentifiersRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

object SecurityRoutes {
    const val BASE_SECURITY_ROUTE = "/security"
}

@Singleton
class SecurityRouter @Inject constructor(
    private val passwordRouter: PasswordRouter,
    private val securityUserIdentifiersRouter: SecurityUserIdentifiersRouter
) : BaseRouter {
    override fun register(route: Route) {
        passwordRouter.register(route)
        securityUserIdentifiersRouter.register(route)
    }
}