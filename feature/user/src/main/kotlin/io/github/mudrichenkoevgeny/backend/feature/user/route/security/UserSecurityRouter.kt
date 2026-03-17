package io.github.mudrichenkoevgeny.backend.feature.user.route.security

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.security.password.PasswordRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.security.useridentifiers.SecurityUserIdentifiersRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates user security routes that require an authenticated user context.
 *
 * Concrete route sets are delegated to [PasswordRouter] and [SecurityUserIdentifiersRouter].
 */
@Singleton
class UserSecurityRouter @Inject constructor(
    private val passwordRouter: PasswordRouter,
    private val securityUserIdentifiersRouter: SecurityUserIdentifiersRouter
) : BaseRouter {
    override fun register(route: Route) {
        passwordRouter.register(route)
        securityUserIdentifiersRouter.register(route)
    }
}