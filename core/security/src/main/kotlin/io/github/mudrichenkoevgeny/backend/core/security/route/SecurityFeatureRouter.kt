package io.github.mudrichenkoevgeny.backend.core.security.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.security.route.management.ManagementSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.core.security.route.open.OpenSecuritySettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates and registers all HTTP routes provided by the `core/security` module.
 */
@Singleton
class SecurityFeatureRouter @Inject constructor(
    private val openSecuritySettingsRouter: OpenSecuritySettingsRouter,
    private val managementSecuritySettingsRouter: ManagementSecuritySettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        openSecuritySettingsRouter.register(route)

        managementSecuritySettingsRouter.register(route) // todo check user permissions and role
    }
}
