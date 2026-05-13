package io.github.mudrichenkoevgeny.backend.feature.securityapi.route.open

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.securityapi.api.route.SecuritySwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.securityapi.api.usecase.open.settings.GetSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.securityapi.network.route.open.security.settings.OpenSecuritySettingsRoutes
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
 * Router for public access to security-related configuration.
 *
 * Registered routes:
 * 1. [OpenSecuritySettingsRoutes.GET_SECURITY_SETTINGS] — retrieves security settings via [GetSecuritySettingsUseCase].
 */
@Singleton
class OpenSecuritySettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getSecuritySettingsUseCase: GetSecuritySettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerGetSecuritySettingsRoute(route)
    }

    private fun registerGetSecuritySettingsRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenSecuritySettingsRoutes.GET_SECURITY_SETTINGS,
            builder = { getSecuritySettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getSecuritySettings() }
        )
    }

    private fun RouteConfig.getSecuritySettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_SECURITY_SETTINGS_ROUTE_SUMMARY
        operationId = GET_SECURITY_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN, SecuritySwaggerTags.SECURITY_SETTINGS)

        description = getFormattedDescription(
            description = GET_SECURITY_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                description = GET_SECURITY_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSecuritySettings() {
        val result = getSecuritySettingsUseCase()

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