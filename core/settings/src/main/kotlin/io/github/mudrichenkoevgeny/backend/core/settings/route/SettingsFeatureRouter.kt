package io.github.mudrichenkoevgeny.backend.core.settings.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.settings.global.route.GlobalSettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry-point router for the settings feature.
 *
 * This router composes settings-related sub-routers and registers them under the provided Ktor
 * [Route].
 */
@Singleton
class SettingsFeatureRouter @Inject constructor(
    private val globalSettingsRouter: GlobalSettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        globalSettingsRouter.register(route)
    }
}