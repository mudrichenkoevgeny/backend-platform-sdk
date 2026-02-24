package io.github.mudrichenkoevgeny.backend.core.settings.global.route

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.mapper.toGlobalSettingsResponse
import io.github.mudrichenkoevgeny.backend.core.settings.global.usecase.GetGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.core.settings.route.SettingsSwaggerTags
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.route.GlobalSettingsRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalSettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getGlobalSettingsUseCase: GetGlobalSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = GlobalSettingsRoutes.GET_GLOBAL_SETTINGS,
            builder = { getGlobalSettingsDocs() },
            body = { getGlobalSettings() }
        )
    }

    private fun RouteConfig.getGlobalSettingsDocs() {
        summary = GET_GLOBAL_SETTINGS_ROUTE_SUMMARY
        description = GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION
        operationId = GET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(SettingsSwaggerTags.GLOBAL_SETTINGS)
        response {
            code(HttpStatusCode.OK) {
                description = GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getGlobalSettings() {
        val result = getGlobalSettingsUseCase.execute()

        call.respondResult(result, appLogger, appErrorParser) { globalSettings ->
            globalSettings.toGlobalSettingsResponse()
        }
    }

    companion object {
        const val GET_GLOBAL_SETTINGS_ROUTE_SUMMARY = "Get global settings"
        const val GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION = "Returns global system settings."
        const val GET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "getGlobalSettings"
        const val GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Global settings data"
    }
}