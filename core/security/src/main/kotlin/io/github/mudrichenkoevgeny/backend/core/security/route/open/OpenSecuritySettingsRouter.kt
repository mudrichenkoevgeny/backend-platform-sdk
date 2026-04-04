package io.github.mudrichenkoevgeny.backend.core.security.route.open

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.security.route.SecuritySwaggerTags
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.settings.GetSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.route.open.security.settings.OpenSecuritySettingsRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP routes for reading security settings.
 */
@Singleton
class OpenSecuritySettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getSecuritySettingsUseCase: GetSecuritySettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = OpenSecuritySettingsRoutes.GET_SECURITY_SETTINGS,
            builder = { getSecuritySettingsDocs() },
            body = { getSecuritySettings() }
        )
    }

    private fun RouteConfig.getSecuritySettingsDocs() {
        summary = GET_SECURITY_SETTINGS_ROUTE_SUMMARY
        description = GET_SECURITY_SETTINGS_ROUTE_DESCRIPTION
        operationId = GET_SECURITY_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN, SecuritySwaggerTags.SECURITY_SETTINGS)
        response {
            code(HttpStatusCode.OK) {
                description = GET_SECURITY_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSecuritySettings() {
        val result = getSecuritySettingsUseCase.execute()

        call.respondResult(result, appLogger, appErrorParser) { securitySettings ->
            securitySettings.toSecuritySettingsPayload()
        }
    }

    companion object {
        const val GET_SECURITY_SETTINGS_ROUTE_SUMMARY = "Get security settings"
        const val GET_SECURITY_SETTINGS_ROUTE_DESCRIPTION = "Returns security settings."
        const val GET_SECURITY_SETTINGS_ROUTE_OPERATION_ID = "getSecuritySettings"
        const val GET_SECURITY_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Security settings data"
    }
}