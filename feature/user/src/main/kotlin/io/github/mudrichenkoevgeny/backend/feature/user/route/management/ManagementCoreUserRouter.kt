package io.github.mudrichenkoevgeny.backend.feature.user.route.management

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.ManagementAuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier.ManagementIdentifierRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.session.ManagementSessionRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementCoreUserRouter @Inject constructor(
    private val managementAuthRouter: ManagementAuthRouter,
    private val managementIdentifierRouter: ManagementIdentifierRouter,
    private val managementSessionRouter: ManagementSessionRouter
) : BaseRouter {
    override fun register(route: Route) {
        managementAuthRouter.register(route)
        managementIdentifierRouter.register(route)
        managementSessionRouter.register(route)
    }
}