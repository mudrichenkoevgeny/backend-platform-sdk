package io.github.mudrichenkoevgeny.backend.feature.user.route.management.configuration

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.configuration.GetUserConfigurationUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.configuration.toUserConfigurationPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.configuration.ManagementUserConfigurationRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for retrieving user feature configuration.
 *
 * Registered routes:
 * 1. [ManagementUserConfigurationRoutes.GET_CONFIGURATION] — retrieves management-specific user settings via [GetUserConfigurationUseCase].
 */
@Singleton
class ManagementUserConfigurationRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getUserConfigurationUseCase: GetUserConfigurationUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerGetUserConfigurationRoute(route)
    }

    private fun registerGetUserConfigurationRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = ManagementUserConfigurationRoutes.GET_CONFIGURATION,
            builder = { getUserConfigurationDocs(allowedRoles, allowedAccountStatuses) },
            body = { getUserConfiguration(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getUserConfigurationDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USER_CONFIGURATION_ROUTE_SUMMARY
        operationId = GET_USER_CONFIGURATION_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER_CONFIGURATION)

        description = getFormattedDescription(
            description = GET_USER_CONFIGURATION_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_CONFIGURATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUserConfiguration(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getUserConfigurationUseCase()

        call.respondResult(result, appLogger, appErrorParser) { userConfiguration ->
            userConfiguration.toUserConfigurationPayload()
        }
    }

    companion object {
        const val GET_USER_CONFIGURATION_ROUTE_SUMMARY = "Get user feature configuration for management"
        const val GET_USER_CONFIGURATION_ROUTE_DESCRIPTION = "Retrieves configuration settings relevant for management tasks and staff workflows."
        const val GET_USER_CONFIGURATION_ROUTE_OPERATION_ID = "getManagementUserConfiguration"
        const val GET_USER_CONFIGURATION_ROUTE_RESPONSE_OK_DESCRIPTION = "Management user configuration data"
    }
}