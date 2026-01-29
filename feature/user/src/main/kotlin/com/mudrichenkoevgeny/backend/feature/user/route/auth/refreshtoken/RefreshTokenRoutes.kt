package io.github.mudrichenkoevgeny.backend.feature.user.route.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import io.github.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.auth.refreshtoken.RefreshTokenRequest
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRoutes
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.refreshtoken.RefreshTokenUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object RefreshTokenRoutes {
    const val REFRESH = "${AuthRoutes.BASE_AUTH_ROUTE}/refresh"
}

@Singleton
class RefreshTokenRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val refreshTokenUseCase: RefreshTokenUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.post(
            path = RefreshTokenRoutes.REFRESH,
            builder = { refreshTokenDocs() },
            body = { refreshToken() }
        )
    }

    private fun RouteConfig.refreshTokenDocs() {
        summary = REFRESH_TOKEN_ROUTE_SUMMARY
        description = REFRESH_TOKEN_ROUTE_DESCRIPTION
        operationId = REFRESH_TOKEN_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<RefreshTokenRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = REFRESH_TOKEN_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.refreshToken() {
        val request = call.validateRequest<RefreshTokenRequest>()

        val result = refreshTokenUseCase.execute(
            refreshToken = RefreshToken(request.refreshToken),
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            sessionToken -> sessionToken.toResponse()
        }
    }

    companion object {
        const val REFRESH_TOKEN_ROUTE_SUMMARY = "refresh auth token"
        const val REFRESH_TOKEN_ROUTE_DESCRIPTION = "Initiates refresh token process."
        const val REFRESH_TOKEN_ROUTE_OPERATION_ID = "refreshToken"
        const val REFRESH_TOKEN_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. Token refreshed."
    }
}