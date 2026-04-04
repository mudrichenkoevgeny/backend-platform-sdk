package io.github.mudrichenkoevgeny.backend.core.security.route.management

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import io.github.mudrichenkoevgeny.backend.core.security.route.SecuritySwaggerTags
import io.github.mudrichenkoevgeny.backend.core.security.usecase.management.settings.UpdateSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.securitysettings.SecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.route.management.security.settings.ManagementSecuritySettingsRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for updating persisted security settings.
 */
@Singleton
class ManagementSecuritySettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val updateSecuritySettingsUseCase: UpdateSecuritySettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.put(
            path = ManagementSecuritySettingsRoutes.UPDATE_SECURITY_SETTINGS,
            builder = { updateSecuritySettingsDocs() },
            body = { updateSecuritySettings() }
        )
    }

    private fun RouteConfig.updateSecuritySettingsDocs() {
        summary = UPDATE_SECURITY_SETTINGS_ROUTE_SUMMARY
        description = UPDATE_SECURITY_SETTINGS_ROUTE_DESCRIPTION
        operationId = UPDATE_SECURITY_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, SecuritySwaggerTags.SECURITY_SETTINGS)
        request {
            body<SecuritySettingsPayload>()
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = UPDATE_SECURITY_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateSecuritySettings() {
        val request = call.validateRequest<SecuritySettingsPayload>()

        val result = updateSecuritySettingsUseCase.execute(
            securitySettings = request.toSecuritySettings()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val UPDATE_SECURITY_SETTINGS_ROUTE_SUMMARY = "Update security settings"
        const val UPDATE_SECURITY_SETTINGS_ROUTE_DESCRIPTION =
            "Replaces effective security settings (e.g. password policy) with the payload."
        const val UPDATE_SECURITY_SETTINGS_ROUTE_OPERATION_ID = "updateSecuritySettings"
        const val UPDATE_SECURITY_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."
    }
}