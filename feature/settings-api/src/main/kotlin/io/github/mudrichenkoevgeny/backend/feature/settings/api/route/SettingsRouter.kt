package io.github.mudrichenkoevgeny.backend.feature.settings.api.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.settings.api.route.management.ManagementGlobalSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.settings.api.route.open.OpenGlobalSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates and registers all HTTP routes provided by the settings API module.
 */
@Singleton
class SettingsRouter @Inject constructor(
    private val openGlobalSettingsRouter: OpenGlobalSettingsRouter,
    private val managementGlobalSettingsRouter: ManagementGlobalSettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        openGlobalSettingsRouter.register(route)

        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            managementGlobalSettingsRouter.register(this)
        }
    }
}
