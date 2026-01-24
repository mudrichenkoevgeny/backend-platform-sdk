package com.mudrichenkoevgeny.backend.feature.user.route.security.password

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.network.request.security.password.PasswordChangeRequest
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.route.security.SecurityRoutes
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.password.PasswordChangeUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object PasswordRoutes {
    const val BASE_PASSWORD_ROUTE = "${SecurityRoutes.BASE_SECURITY_ROUTE}/password"
    const val PASSWORD_CHANGE = "$BASE_PASSWORD_ROUTE/change"
}

@Singleton
class PasswordRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val passwordChangeUseCase: PasswordChangeUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.post(
            path = PasswordRoutes.PASSWORD_CHANGE,
            builder = { passwordChangeDocs() },
            body = { passwordChange() }
        )
    }

    private fun RouteConfig.passwordChangeDocs() {
        summary = PASSWORD_CHANGE_SUMMARY
        description = PASSWORD_CHANGE_DESCRIPTION
        operationId = PASSWORD_CHANGE_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
        request {
            body<PasswordChangeRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = PASSWORD_CHANGE_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.passwordChange() {
        val request = call.validateRequest<PasswordChangeRequest>()

        val result = passwordChangeUseCase.execute(
            email = request.email,
            oldPassword = request.oldPassword,
            newPassword = request.newPassword,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val PASSWORD_CHANGE_SUMMARY = "Change password"
        const val PASSWORD_CHANGE_DESCRIPTION = "Changes the current user's password."
        const val PASSWORD_CHANGE_OPERATION_ID = "passwordChange"
        const val PASSWORD_CHANGE_RESPONSE_DESCRIPTION = "Password changed successfully."
    }
}