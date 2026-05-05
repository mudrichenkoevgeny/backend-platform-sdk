package io.github.mudrichenkoevgeny.backend.feature.user.route.management.user

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.GetUserUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserDetailsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.user.SelfManagementUserRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for self-service user profile operations.
 *
 * Registered routes:
 * 1. [SelfManagementUserRoutes.GET_USER] — retrieves detailed information about the currently authenticated user via [GetUserUseCase].
 */
@Singleton
class SelfManagementUserRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getUserUseCase: GetUserUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetUserRoute(this)
        }
    }

    private fun registerGetUserRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = SelfManagementUserRoutes.GET_USER,
            builder = { getUserDocs(allowedRoles, allowedAccountStatuses) },
            body = { getUser(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USER_ROUTE_SUMMARY
        operationId = GET_USER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER)

        description = getFormattedDescription(
            description = GET_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUser(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getUserUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userDetails ->
            userDetails.toUserDetailsPayload()
        }
    }

    companion object {
        const val GET_USER_ROUTE_SUMMARY = "Get current user"
        const val GET_USER_ROUTE_DESCRIPTION = "Returns information about the currently authenticated user."
        const val GET_USER_ROUTE_OPERATION_ID = "getUser"
        const val GET_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User data retrieved successfully"
    }
}