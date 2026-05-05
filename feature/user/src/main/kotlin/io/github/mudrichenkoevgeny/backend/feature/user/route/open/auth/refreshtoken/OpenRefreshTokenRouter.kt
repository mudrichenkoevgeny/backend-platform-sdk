package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.refreshtoken.RefreshTokenUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.token.toSessionTokenPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.token.RefreshTokenPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.refreshtoken.OpenRefreshTokenRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public authentication routes for session token renewal.
 *
 * Registered routes:
 * 1. [OpenRefreshTokenRoutes.REFRESH_TOKEN] — issues new access and refresh tokens using an existing valid refresh token via [RefreshTokenUseCase].
 */
@Singleton
class OpenRefreshTokenRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val refreshTokenUseCase: RefreshTokenUseCase
) : BaseRouter {
    override fun register(route: Route) {
        registerRefreshTokenRoute(route)
    }

    private fun registerRefreshTokenRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenRefreshTokenRoutes.REFRESH_TOKEN,
            builder = { refreshTokenDocs(allowedRoles, allowedAccountStatuses) },
            body = { refreshToken() }
        )
    }

    private fun RouteConfig.refreshTokenDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = REFRESH_TOKEN_ROUTE_SUMMARY
        operationId = REFRESH_TOKEN_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = REFRESH_TOKEN_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<RefreshTokenPayload>() }
        response {
            code(HttpStatusCode.OK) {
                description = REFRESH_TOKEN_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.refreshToken() {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<RefreshTokenPayload>()

        val result = refreshTokenUseCase(
            refreshToken = RefreshToken(request.refreshToken),
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { sessionToken ->
            sessionToken.toSessionTokenPayload()
        }
    }

    companion object {
        const val REFRESH_TOKEN_ROUTE_SUMMARY = "refresh auth token"
        const val REFRESH_TOKEN_ROUTE_DESCRIPTION = "Initiates refresh token process."
        const val REFRESH_TOKEN_ROUTE_OPERATION_ID = "refreshToken"
        const val REFRESH_TOKEN_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. Token refreshed."
    }
}