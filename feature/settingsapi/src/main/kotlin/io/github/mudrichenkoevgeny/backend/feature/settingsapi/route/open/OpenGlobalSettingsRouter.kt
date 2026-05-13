package io.github.mudrichenkoevgeny.backend.feature.settingsapi.route.open

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.api.route.SettingsSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.api.usecase.open.globalsettings.GetGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.network.route.open.globalsettings.OpenGlobalSettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router for public access to system-wide configuration.
 *
 * Registered routes:
 * 1. [OpenGlobalSettingsRoutes.GET_GLOBAL_SETTINGS] — retrieves global settings via [GetGlobalSettingsUseCase].
 */
@Singleton
class OpenGlobalSettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getGlobalSettingsUseCase: GetGlobalSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerGetGlobalSettingsRoute(route)
    }

    private fun registerGetGlobalSettingsRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenGlobalSettingsRoutes.GET_GLOBAL_SETTINGS,
            builder = { getGlobalSettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getGlobalSettings() }
        )
    }

    private fun RouteConfig.getGlobalSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_GLOBAL_SETTINGS_ROUTE_SUMMARY
        operationId = GET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN, SettingsSwaggerTags.GLOBAL_SETTINGS)

        description = getFormattedDescription(
            description = GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                description = GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getGlobalSettings() {
        val result = getGlobalSettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { globalSettings ->
            globalSettings.toGlobalSettingsPayload()
        }
    }

    companion object {
        const val GET_GLOBAL_SETTINGS_ROUTE_SUMMARY = "Get global settings"
        const val GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION = "Returns global system settings."
        const val GET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "getGlobalSettings"
        const val GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Global settings data"
    }
}
