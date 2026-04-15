package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.settings.ManagementAuthSettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementAuthRouter @Inject constructor(
    private val managementAuthSettingsRouter: ManagementAuthSettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        managementAuthSettingsRouter.register(route)
    }
}