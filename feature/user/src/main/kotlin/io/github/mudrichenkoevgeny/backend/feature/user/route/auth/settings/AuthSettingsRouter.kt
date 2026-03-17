package io.github.mudrichenkoevgeny.backend.feature.user.route.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth.toAuthSettingsResponse
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.settings.GetAuthSettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.auth.settings.AuthSettingsRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public auth settings endpoint.
 *
 * Exposes authentication-related configuration required by clients (e.g., enabled providers).
 */
@Singleton
class AuthSettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getAuthSettingsUseCase: GetAuthSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = AuthSettingsRoutes.GET_AUTH_SETTINGS,
            builder = { getAuthSettingsDocs() },
            body = { getAuthSettings() }
        )
    }

    private fun RouteConfig.getAuthSettingsDocs() {
        summary = GET_AUTH_SETTINGS_ROUTE_SUMMARY
        description = GET_AUTH_SETTINGS_ROUTE_DESCRIPTION
        operationId = GET_AUTH_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH_SETTINGS)
        response {
            code(HttpStatusCode.OK) {
                description = GET_AUTH_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuthSettings() {
        val result = getAuthSettingsUseCase.execute()

        call.respondResult(result, appLogger, appErrorParser) { authSettings ->
            authSettings.toAuthSettingsResponse()
        }
    }

    companion object {
        const val GET_AUTH_SETTINGS_ROUTE_SUMMARY = "Get auth settings"
        const val GET_AUTH_SETTINGS_ROUTE_DESCRIPTION = "Returns available authentication settings."
        const val GET_AUTH_SETTINGS_ROUTE_OPERATION_ID = "getAuthSettings"
        const val GET_AUTH_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Auth settings data"
    }
}