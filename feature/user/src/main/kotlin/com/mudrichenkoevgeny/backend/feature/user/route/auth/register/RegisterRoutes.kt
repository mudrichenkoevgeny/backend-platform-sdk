package com.mudrichenkoevgeny.backend.feature.user.route.auth.register

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.auth.register.RegisterByEmailRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRoutes
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.register.RegisterByEmailUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object RegisterRoutes {
    const val BASE_REGISTER_ROUTE = "${AuthRoutes.BASE_AUTH_ROUTE}/register"
    const val REGISTER_BY_EMAIL = "$BASE_REGISTER_ROUTE/email"
}

@Singleton
class RegisterRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val registerByEmailUseCase: RegisterByEmailUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.post(
            path = RegisterRoutes.REGISTER_BY_EMAIL,
            builder = { registerByEmailDocs() },
            body = { registerByEmail() }
        )
    }

    private fun RouteConfig.registerByEmailDocs() {
        summary = REGISTER_BY_EMAIL_ROUTE_SUMMARY
        description = REGISTER_BY_EMAIL_ROUTE_DESCRIPTION
        operationId = REGISTER_BY_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<RegisterByEmailRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = REGISTER_BY_EMAIL_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.registerByEmail() {
        val request = call.validateRequest<RegisterByEmailRequest>()

        val result = registerByEmailUseCase.execute(
            email = request.email,
            password = request.password,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val REGISTER_BY_EMAIL_ROUTE_SUMMARY = "Register user by email"
        const val REGISTER_BY_EMAIL_ROUTE_DESCRIPTION = "Registers a new user using email and password " +
                "after confirmation code validation."
        const val REGISTER_BY_EMAIL_ROUTE_OPERATION_ID = "registerByEmail"
        const val REGISTER_BY_EMAIL_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. User registered."
    }
}