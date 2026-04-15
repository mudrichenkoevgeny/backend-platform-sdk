package io.github.mudrichenkoevgeny.backend.feature.user.route.open.configuration

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.configuration.GetUserConfigurationUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.configuration.UserConfigurationRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public configuration endpoint for the user feature.
 *
 * Exposes combined configuration needed by clients (e.g., auth settings, security policies).
 * This endpoint is intentionally public to allow clients to bootstrap their auth flows.
 */
@Singleton
class UserConfigurationRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getUserConfigurationUseCase: GetUserConfigurationUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = UserConfigurationRoutes.GET_CONFIGURATION,
            builder = { getUserConfigurationDocs() },
            body = { getUserConfiguration() }
        )
    }

    private fun RouteConfig.getUserConfigurationDocs() {
        summary = GET_USER_CONFIGURATION_ROUTE_SUMMARY
        description = GET_USER_CONFIGURATION_ROUTE_DESCRIPTION
        operationId = GET_USER_CONFIGURATION_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER_CONFIGURATION)
        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_CONFIGURATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUserConfiguration() {
        val result = getUserConfigurationUseCase.execute()

        call.respondResult(result, appLogger, appErrorParser) { userConfiguration ->
            userConfiguration.toUserConfigurationResponse()
        }
    }

    companion object {
        const val GET_USER_CONFIGURATION_ROUTE_SUMMARY = "Get all user feature configuration"
        const val GET_USER_CONFIGURATION_ROUTE_DESCRIPTION = "Returns combined global, security, and auth settings for the user module."
        const val GET_USER_CONFIGURATION_ROUTE_OPERATION_ID = "getUserConfiguration"
        const val GET_USER_CONFIGURATION_ROUTE_RESPONSE_OK_DESCRIPTION = "Combined user configuration data"
    }
}