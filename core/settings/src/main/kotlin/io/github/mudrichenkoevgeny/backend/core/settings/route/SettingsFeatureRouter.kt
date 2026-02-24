package io.github.mudrichenkoevgeny.backend.core.settings.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.settings.global.route.GlobalSettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsFeatureRouter @Inject constructor(
    private val globalSettingsRouter: GlobalSettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        globalSettingsRouter.register(route)
    }
}