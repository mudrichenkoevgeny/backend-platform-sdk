package io.github.mudrichenkoevgeny.backend.core.security.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.security.settings.route.SecuritySettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityFeatureRouter @Inject constructor(
    private val securitySettingsRouter: SecuritySettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        securitySettingsRouter.register(route)
    }
}