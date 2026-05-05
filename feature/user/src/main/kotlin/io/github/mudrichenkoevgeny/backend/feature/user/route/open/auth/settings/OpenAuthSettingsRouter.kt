package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.settings.GetAuthSettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.settings.OpenAuthSettingsRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public authentication settings routes providing system-wide auth configurations.
 *
 * Registered routes:
 * 1. [OpenAuthSettingsRoutes.GET_AUTH_SETTINGS] — retrieves global authentication configurations (e.g., enabled providers) via [GetAuthSettingsUseCase].
 */
@Singleton
class OpenAuthSettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getAuthSettingsUseCase: GetAuthSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerGetAuthSettingsRoute(route)
    }

    private fun registerGetAuthSettingsRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenAuthSettingsRoutes.GET_AUTH_SETTINGS,
            builder = { getAuthSettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getAuthSettings() }
        )
    }

    private fun RouteConfig.getAuthSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_AUTH_SETTINGS_ROUTE_SUMMARY
        operationId = GET_AUTH_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH_SETTINGS)

        description = getFormattedDescription(
            description = GET_AUTH_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                description = GET_AUTH_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuthSettings() {
        val result = getAuthSettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { authSettings ->
            authSettings.toAuthSettingsPayload()
        }
    }

    companion object {
        const val GET_AUTH_SETTINGS_ROUTE_SUMMARY = "Get auth settings"
        const val GET_AUTH_SETTINGS_ROUTE_DESCRIPTION = "Returns available authentication settings."
        const val GET_AUTH_SETTINGS_ROUTE_OPERATION_ID = "getAuthSettings"
        const val GET_AUTH_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Auth settings data"
    }
}