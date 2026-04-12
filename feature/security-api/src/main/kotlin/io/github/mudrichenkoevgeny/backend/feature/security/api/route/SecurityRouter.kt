package io.github.mudrichenkoevgeny.backend.feature.security.api.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.security.api.route.management.ManagementSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.security.api.route.open.OpenSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates and registers all HTTP routes provided by the `core/security` module.
 */
@Singleton
class SecurityRouter @Inject constructor(
    private val openSecuritySettingsRouter: OpenSecuritySettingsRouter,
    private val managementSecuritySettingsRouter: ManagementSecuritySettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        openSecuritySettingsRouter.register(route)

        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            managementSecuritySettingsRouter.register(this)
        }
    }
}
